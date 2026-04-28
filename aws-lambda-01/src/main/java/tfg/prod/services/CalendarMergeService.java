package tfg.prod.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;
import tfg.prod.modules.MergeConfig;
import tfg.prod.modules.MergedEvent;
import tfg.prod.modules.MergeSource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class CalendarMergeService {

    private final GoogleTokenService googleTokenService;
    private final CalDavSessionService calDavSessionService;
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public CalendarMergeService(GoogleTokenService googleTokenService,
                                CalDavSessionService calDavSessionService) {
        this.googleTokenService  = googleTokenService;
        this.calDavSessionService = calDavSessionService;
    }

    public List<MergedEvent> merge(MergeConfig config) {
        List<MergedEvent> all = new ArrayList<>();
        for (MergeSource source : config.getSources()) {
            try {
                if ("google".equals(source.getProvider())) {
                    all.addAll(fetchGoogle(source, config.getMaxResultsPerCalendar()));
                } else if ("caldav".equals(source.getProvider())) {
                    all.addAll(fetchCalDav(source));
                }
            } catch (Exception e) {
                System.err.println("Error en fuente " + source.getAccountId() + ": " + e.getMessage());
            }
        }
        return all;
    }

    // ---- Google ----

    private List<MergedEvent> fetchGoogle(MergeSource source, int maxResults) throws Exception {
        Credential credential = googleTokenService.getCredential(source.getAccountId());
        Calendar service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName("TFG Calendar Merger").build();

        Events events = service.events().list(source.getCalendarId())
                .setMaxResults(maxResults)
                .setSingleEvents(true)
                .setOrderBy("startTime")
                .execute();

        List<MergedEvent> result = new ArrayList<>();
        for (Event e : events.getItems()) {
            String title = e.getSummary() != null ? e.getSummary() : "(sin título)";
            if (source.getPrefix() != null) title = source.getPrefix() + title;

            EventDateTime startDt = e.getStart();
            EventDateTime endDt   = e.getEnd();
            boolean allDay = startDt.getDateTime() == null;
            String start = allDay
                    ? rfc3339ToIcs(startDt.getDate().toStringRfc3339())
                    : rfc3339ToIcs(startDt.getDateTime().toStringRfc3339());
            String end = allDay
                    ? rfc3339ToIcs(endDt.getDate().toStringRfc3339())
                    : rfc3339ToIcs(endDt.getDateTime().toStringRfc3339());

            result.add(new MergedEvent(e.getId(), title, e.getDescription(),
                    e.getLocation(), start, end, allDay));
        }
        return result;
    }

    // ---- CalDAV ----

    private List<MergedEvent> fetchCalDav(MergeSource source) throws Exception {
        String baseUrl = calDavSessionService.getServerUrl(source.getAccountId());
        String user    = calDavSessionService.getUsername(source.getAccountId());
        String pass    = calDavSessionService.getPassword(source.getAccountId());
        String calUrl  = source.getCalendarId().startsWith("http")
                ? source.getCalendarId()
                : baseUrl + "/" + source.getCalendarId();

        String reportBody = """
            <?xml version="1.0" encoding="UTF-8"?>
            <C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
              <D:prop><D:getetag/><C:calendar-data/></D:prop>
              <C:filter>
                <C:comp-filter name="VCALENDAR">
                  <C:comp-filter name="VEVENT"/>
                </C:comp-filter>
              </C:filter>
            </C:calendar-query>
            """;

        String xml = sendReport(calUrl, user, pass, reportBody);
        return parseCalDavResponse(xml, source.getPrefix());
    }

    private List<MergedEvent> parseCalDavResponse(String xml, String prefix) {
        List<MergedEvent> events = new ArrayList<>();
        int start = 0;
        while (true) {
            int tagStart = xml.indexOf("<calendar-data", start);
            if (tagStart == -1) tagStart = xml.indexOf("<C:calendar-data", start);
            if (tagStart == -1) break;
            int contentStart = xml.indexOf(">", tagStart) + 1;
            int contentEnd   = xml.indexOf("</", contentStart);
            if (contentEnd == -1) break;
            String ics = xml.substring(contentStart, contentEnd).trim();
            if (!ics.isBlank()) {
                MergedEvent ev = parseIcs(ics, prefix);
                if (ev != null) events.add(ev);
            }
            start = contentEnd + 1;
        }
        return events;
    }

    private MergedEvent parseIcs(String icsData, String prefix) {
        try {
            CalendarBuilder builder = new CalendarBuilder();
            net.fortuna.ical4j.model.Calendar cal = builder.build(new StringReader(icsData));
            for (Object component : cal.getComponents()) {
                if (!(component instanceof VEvent e)) continue;
                String uid     = e.getUid()        != null ? e.getUid().getValue()        : java.util.UUID.randomUUID().toString();
                String summary = e.getSummary()     != null ? e.getSummary().getValue()     : "(sin título)";
                if (prefix != null) summary = prefix + summary;
                String desc    = e.getDescription() != null ? e.getDescription().getValue() : null;
                String loc     = e.getLocation()    != null ? e.getLocation().getValue()    : null;
                String startStr = e.getStartDate()  != null ? e.getStartDate().getValue().toString() : "";
                String endStr   = e.getEndDate()    != null ? e.getEndDate().getValue().toString()   : startStr;
                boolean allDay  = !startStr.contains("T");
                return new MergedEvent(uid, summary, desc, loc, startStr, endStr, allDay);
            }
        } catch (Exception e) {
            System.err.println("Error parseando ICS: " + e.getMessage());
        }
        return null;
    }

    // ---- Helpers ----

    private String rfc3339ToIcs(String rfc3339) {
        if (rfc3339 == null) return "";
        return rfc3339.replaceAll("[\\-:]", "").replaceAll("\\.\\d+", "").substring(0, rfc3339.contains("T") ? 16 : 8);
    }

    private String sendReport(String url, String user, String pass, String body) throws Exception {
        String auth = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        try (CloseableHttpClient client = HttpClients.custom()
                .addRequestInterceptorFirst((req, e, ctx) -> req.setHeader("Authorization", "Basic " + auth))
                .build()) {
            HttpUriRequestBase request = new HttpUriRequestBase("REPORT", java.net.URI.create(url));
            request.setHeader("Content-Type", "application/xml; charset=UTF-8");
            request.setHeader("Depth", "1");
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_XML));
            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
}
