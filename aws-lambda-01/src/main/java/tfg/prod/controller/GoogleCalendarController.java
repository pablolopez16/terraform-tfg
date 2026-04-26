package tfg.prod.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Value;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tfg.prod.CredentialsLoader;
import tfg.prod.modules.GoogleCredentials;
import tfg.prod.services.GoogleTokenService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/google-calendar")
public class GoogleCalendarController {

    private static final String APPLICATION_NAME = "TFG Calendar Merger";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GoogleTokenService tokenService;
    @Value("${google.redirect-uri}")
    private String redirectUri;

    public GoogleCalendarController(GoogleTokenService tokenService) {
        this.tokenService = tokenService;
    }

    // Devuelve la URL de autorización de Google. account_id se pasa como state para recuperarlo en el callback.
    @GetMapping("/auth/google")
    public ResponseEntity<String> redirectToGoogle(@RequestParam String accountId) {
        GoogleCredentials creds = CredentialsLoader.load();
        String url = new GoogleAuthorizationCodeRequestUrl(
                creds.getClient_id(),
                redirectUri,
                creds.getScopes()
        ).setAccessType("offline")
         .set("prompt", "consent")
         .setState(accountId)
         .build();
        return ResponseEntity.ok(url);
    }

    // Google redirige aquí con code y state (=accountId) tras la autenticación
    @GetMapping("/auth/google/callback")
    public ResponseEntity<?> handleGoogleCallback(
            @RequestParam String code,
            @RequestParam(required = false, defaultValue = "default") String state) {
        String accountId = state;
        try {
            GoogleCredentials creds = CredentialsLoader.load();
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    transport, JSON_FACTORY,
                    "https://oauth2.googleapis.com/token",
                    creds.getClient_id(), creds.getClient_secret(),
                    code, redirectUri
            ).execute();
            tokenService.saveToken(accountId, tokenResponse);
            return ResponseEntity.ok(Map.of(
                    "message",       "Autenticación completada",
                    "account_id",    accountId,
                    "access_token",  tokenResponse.getAccessToken(),
                    "refresh_token", tokenResponse.getRefreshToken() != null
                            ? tokenResponse.getRefreshToken()
                            : "no devuelto (ya existe en DynamoDB)"
            ));
        } catch (TokenResponseException e) {
            return ResponseEntity.status(500).body("Error de token: " + e.getDetails());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // Inicializa el token desde un refresh_token (útil tras cold start o desde otros servicios)
    @PostMapping("/auth/token")
    public ResponseEntity<?> getTokenFromRefresh(@RequestBody Map<String, String> body) {
        String accountId    = body.get("account_id");
        String refreshToken = body.get("refresh_token"); // opcional si ya está en DynamoDB
        if (accountId == null || accountId.isBlank()) {
            return ResponseEntity.badRequest().body("Falta el campo account_id");
        }
        try {
            String accessToken = tokenService.initFromRefreshToken(accountId, refreshToken);
            return ResponseEntity.ok(Map.of("account_id", accountId, "access_token", accessToken));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{accountId}/calendars")
    public ResponseEntity<?> listCalendars(@PathVariable String accountId) {
        if (!tokenService.hasToken(accountId)) {
            return ResponseEntity.status(401).body(
                    "No autenticado. Usa GET /google-calendar/auth/google?accountId=" + accountId);
        }
        try {
            Calendar service = buildCalendarService(accountId);
            CalendarList calendarList = service.calendarList().list().execute();
            List<CalendarListEntry> items = calendarList.getItems();
            if (items == null || items.isEmpty()) return ResponseEntity.ok("No se encontraron calendarios.");
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{accountId}/calendars/{calendarId}/events")
    public ResponseEntity<?> listEvents(
            @PathVariable String accountId,
            @PathVariable String calendarId,
            @RequestParam(defaultValue = "50") int maxResults) {
        if (!tokenService.hasToken(accountId)) {
            return ResponseEntity.status(401).body("No autenticado.");
        }
        try {
            Calendar service = buildCalendarService(accountId);
            Events events = service.events().list(calendarId)
                    .setMaxResults(maxResults)
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();
            List<Event> items = events.getItems();
            if (items == null || items.isEmpty()) return ResponseEntity.ok("No se encontraron eventos.");
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private Calendar buildCalendarService(String accountId) throws Exception {
        Credential credential = tokenService.getCredential(accountId);
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential
        ).setApplicationName(APPLICATION_NAME).build();
    }
}
