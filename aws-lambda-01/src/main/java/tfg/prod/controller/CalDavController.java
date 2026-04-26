package tfg.prod.controller;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tfg.prod.services.CalDavSessionService;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/caldav")
public class CalDavController {

    private final CalDavSessionService sessionService;

    public CalDavController(CalDavSessionService sessionService) {
        this.sessionService = sessionService;
    }

    // ---------------------------------------------------------------
    // AUTH
    // ---------------------------------------------------------------

    @PostMapping("/auth/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> body) {
        String accountId = body.get("account_id");
        String username  = body.get("username");
        String password  = body.get("password");
        String serverUrl = body.get("serverUrl");

        if (accountId == null || username == null || password == null || serverUrl == null) {
            return ResponseEntity.badRequest().body("Faltan campos: account_id, username, password, serverUrl");
        }
        try {
            boolean valid = testCredentials(username, password, serverUrl);
            if (!valid) {
                return ResponseEntity.status(401).body("Credenciales incorrectas o servidor no accesible");
            }
            sessionService.saveSession(accountId, username, password, serverUrl);
            return ResponseEntity.ok("Sesión CalDAV iniciada para: " + accountId);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<String> logout(@RequestBody Map<String, String> body) {
        String accountId = body.get("account_id");
        if (accountId == null) return ResponseEntity.badRequest().body("Falta account_id");
        sessionService.clearSession(accountId);
        return ResponseEntity.ok("Sesión CalDAV cerrada para: " + accountId);
    }

    @GetMapping("/auth/status")
    public ResponseEntity<?> status(@RequestParam String accountId) {
        if (!sessionService.hasSession(accountId)) {
            return ResponseEntity.status(401).body("No hay sesión activa para: " + accountId);
        }
        return ResponseEntity.ok(Map.of(
                "account_id", accountId,
                "username",   sessionService.getUsername(accountId),
                "serverUrl",  sessionService.getServerUrl(accountId),
                "status",     "autenticado"
        ));
    }

    // ---------------------------------------------------------------
    // CALENDARIOS
    // ---------------------------------------------------------------

    @GetMapping("/{accountId}/calendars")
    public ResponseEntity<?> listCalendars(@PathVariable String accountId) {
        if (!sessionService.hasSession(accountId)) {
            return ResponseEntity.status(401).body("No autenticado. Usa POST /caldav/auth/login");
        }
        try {
            String url  = sessionService.getServerUrl(accountId);
            String user = sessionService.getUsername(accountId);
            String pass = sessionService.getPassword(accountId);

            String propfindBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <D:propfind xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav"
                            xmlns:CS="http://calendarserver.org/ns/">
                  <D:prop>
                    <D:displayname/>
                    <C:calendar-description/>
                    <CS:getctag/>
                    <D:resourcetype/>
                  </D:prop>
                </D:propfind>
                """;

            String response = sendPropfind(url, user, pass, propfindBody, "1");
            List<Map<String, String>> calendars = parseCalendarsFromXml(response, url);
            if (calendars.isEmpty()) return ResponseEntity.ok("No se encontraron calendarios.");
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // EVENTOS
    // ---------------------------------------------------------------

    @GetMapping("/{accountId}/calendars/events")
    public ResponseEntity<?> listEvents(@PathVariable String accountId, @RequestParam String calendarPath) {
        if (!sessionService.hasSession(accountId)) {
            return ResponseEntity.status(401).body("No autenticado. Usa POST /caldav/auth/login");
        }
        try {
            String baseUrl = sessionService.getServerUrl(accountId);
            String user    = sessionService.getUsername(accountId);
            String pass    = sessionService.getPassword(accountId);
            String calendarUrl = buildCalendarUrl(baseUrl, calendarPath);

            String reportBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
                  <D:prop>
                    <D:getetag/>
                    <C:calendar-data/>
                  </D:prop>
                  <C:filter>
                    <C:comp-filter name="VCALENDAR">
                      <C:comp-filter name="VEVENT"/>
                    </C:comp-filter>
                  </C:filter>
                </C:calendar-query>
                """;

            String response = sendReport(calendarUrl, user, pass, reportBody);
            List<Map<String, Object>> events = parseEventsFromResponse(response);
            if (events.isEmpty()) return ResponseEntity.ok("No se encontraron eventos.");
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVADOS
    // ---------------------------------------------------------------

    private boolean testCredentials(String username, String password, String serverUrl) throws Exception {
        String propfind = """
            <?xml version="1.0" encoding="UTF-8"?>
            <D:propfind xmlns:D="DAV:">
              <D:prop><D:current-user-principal/></D:prop>
            </D:propfind>
            """;
        try (CloseableHttpClient client = buildHttpClient(username, password)) {
            HttpUriRequestBase request = new HttpUriRequestBase("PROPFIND", java.net.URI.create(serverUrl));
            request.setHeader("Content-Type", "application/xml; charset=UTF-8");
            request.setHeader("Depth", "0");
            request.setEntity(new StringEntity(propfind, ContentType.APPLICATION_XML));
            try (CloseableHttpResponse response = client.execute(request)) {
                int status = response.getCode();
                return status == 207 || status == 200;
            }
        }
    }

    private String sendPropfind(String url, String user, String pass, String body, String depth) throws Exception {
        try (CloseableHttpClient client = buildHttpClient(user, pass)) {
            HttpUriRequestBase request = new HttpUriRequestBase("PROPFIND", java.net.URI.create(url));
            request.setHeader("Content-Type", "application/xml; charset=UTF-8");
            request.setHeader("Depth", depth);
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_XML));
            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }

    private String sendReport(String url, String user, String pass, String body) throws Exception {
        try (CloseableHttpClient client = buildHttpClient(user, pass)) {
            HttpUriRequestBase request = new HttpUriRequestBase("REPORT", java.net.URI.create(url));
            request.setHeader("Content-Type", "application/xml; charset=UTF-8");
            request.setHeader("Depth", "1");
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_XML));
            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }

    private CloseableHttpClient buildHttpClient(String user, String pass) {
        String credentials = Base64.getEncoder().encodeToString(
                (user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        return HttpClients.custom()
                .addRequestInterceptorFirst((request, entity, context) ->
                        request.setHeader("Authorization", "Basic " + credentials))
                .build();
    }

    private List<Map<String, String>> parseCalendarsFromXml(String xml, String baseUrl) {
        List<Map<String, String>> calendars = new ArrayList<>();
        String[] responses = xml.split("<[^>]*:?response>");
        for (String resp : responses) {
            if (!resp.contains("calendar") || !resp.contains("href")) continue;
            Map<String, String> cal = new LinkedHashMap<>();
            String href = extractXmlValue(resp, "href");
            if (href == null || href.isBlank()) continue;
            cal.put("href", href);
            String name = extractXmlValue(resp, "displayname");
            cal.put("name", name != null ? name : href);
            calendars.add(cal);
        }
        return calendars;
    }

    private List<Map<String, Object>> parseEventsFromResponse(String xml) {
        List<Map<String, Object>> events = new ArrayList<>();
        int start = 0;
        while (true) {
            int tagStart = xml.indexOf("<calendar-data", start);
            if (tagStart == -1) tagStart = xml.indexOf("<C:calendar-data", start);
            if (tagStart == -1) break;
            int contentStart = xml.indexOf(">", tagStart) + 1;
            int contentEnd   = xml.indexOf("</", contentStart);
            if (contentEnd == -1) break;
            String icsData = xml.substring(contentStart, contentEnd).trim();
            if (!icsData.isBlank()) {
                Map<String, Object> event = parseIcsEvent(icsData);
                if (event != null) events.add(event);
            }
            start = contentEnd + 1;
        }
        return events;
    }

    private Map<String, Object> parseIcsEvent(String icsData) {
        try {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(new StringReader(icsData));
            for (Object component : calendar.getComponents()) {
                if (!(component instanceof VEvent event)) continue;
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id",          event.getUid()         != null ? event.getUid().getValue()         : "unknown");
                map.put("summary",     event.getSummary()      != null ? event.getSummary().getValue()      : "(sin título)");
                map.put("start",       event.getStartDate()    != null ? event.getStartDate().getValue()    : null);
                map.put("end",         event.getEndDate()      != null ? event.getEndDate().getValue()      : null);
                map.put("description", event.getDescription()  != null ? event.getDescription().getValue()  : null);
                map.put("location",    event.getLocation()     != null ? event.getLocation().getValue()     : null);
                return map;
            }
        } catch (Exception e) {
            System.err.println("Error parseando evento ICS: " + e.getMessage());
        }
        return null;
    }

    private String extractXmlValue(String xml, String tagName) {
        for (String tag : new String[]{tagName, "D:" + tagName, "C:" + tagName}) {
            int start = xml.indexOf("<" + tag + ">");
            if (start == -1) continue;
            int end = xml.indexOf("</" + tag + ">", start);
            if (end == -1) continue;
            return xml.substring(start + tag.length() + 2, end).trim();
        }
        return null;
    }

    private String buildCalendarUrl(String baseUrl, String calendarPath) {
        if (calendarPath.startsWith("http")) return calendarPath;
        if (calendarPath.startsWith("/")) {
            int pathStart = baseUrl.indexOf("/", 8);
            String host = pathStart > 0 ? baseUrl.substring(0, pathStart) : baseUrl;
            return host + calendarPath;
        }
        return baseUrl + "/" + calendarPath;
    }
}
