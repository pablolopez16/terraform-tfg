package tfg.prod.controller;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.http.HttpTransport;
import tfg.prod.CredentialsLoader;
import tfg.prod.modules.*;
import tfg.prod.services.GoogleTokenService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/google-calendar")
public class GoogleCalendarController {

    private static final String APPLICATION_NAME = "TFG Calendar Merger";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    GoogleCredentials creds = CredentialsLoader.load();

    String clientId = creds.getClient_id();
    String clientSecret = creds.getClient_secret();
    String redirectUri = creds.getRedirect_uri();
    String scopes = String.join(" ", creds.getScopes());

    private final GoogleTokenService tokenService;

    public GoogleCalendarController(GoogleTokenService tokenService) {
        this.tokenService = tokenService;
    }

@GetMapping("/auth/google")
public ResponseEntity<String> redirectToGoogle() {

   GoogleCredentials creds = CredentialsLoader.load();


   GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(
    creds.getClient_id(),
    creds.getRedirect_uri(),
    creds.getScopes()
    ).setAccessType("offline").set("prompt", "consent");
    url.set("prompt", "consent");
    return ResponseEntity.ok(url.build());
}

@GetMapping("/auth/google/callback")
public ResponseEntity<String> handleGoogleCallback(@RequestParam String code) {
    try {
        GoogleCredentials creds = CredentialsLoader.load();

        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        // Intercambiamos el código directamente sin usar GoogleAuthorizationCodeFlow
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                transport,
                jsonFactory,
                "https://oauth2.googleapis.com/token",
                creds.getClient_id(),
                creds.getClient_secret(),
                code,
                creds.getRedirect_uri()
        ).execute();

        tokenService.saveToken(tokenResponse);

        return ResponseEntity.ok("Autenticación completada. Ya puedes usar /google-calendar/calendars");

    } catch (TokenResponseException e) {
        return ResponseEntity.status(500).body("Error de token: " + e.getDetails());
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("Error al obtener el token: " + e.getMessage());
    }
}

// ---------------------------------------------------------------
    // CALENDARIOS
// ---------------------------------------------------------------

/**
     * Lista todos los calendarios de la cuenta autenticada.
     *
     * Requiere haber completado el flujo OAuth2 previamente.
     * Devuelve un array JSON con los calendarios: id, summary, description, primary, etc.
     *
     * Ejemplo de uso:
     *   GET /google-calendar/calendars
     */
@GetMapping("/calendars")
    public ResponseEntity<?> listCalendars() {
        if (!tokenService.hasToken()) {
            return ResponseEntity.status(401).body(
                "No autenticado. Visita primero /google-calendar/auth/google"
            );
        }

        try {
            Calendar service = buildCalendarService();

            CalendarList calendarList = service.calendarList().list().execute();
            List<CalendarListEntry> items = calendarList.getItems();

            if (items == null || items.isEmpty()) {
                return ResponseEntity.ok("No se encontraron calendarios en esta cuenta.");
            }

            return ResponseEntity.ok(items);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al listar calendarios: " + e.getMessage());
        }
    }
/** 
     * Ejemplo de uso:
     *   GET /google-calendar/calendars/primary/events
     *   GET /google-calendar/calendars/primary/events?maxResults=10
     *   GET /google-calendar/calendars/abc123%40group.calendar.google.com/events
     *
     * Nota: los IDs con @ deben ir URL-encoded en la petición.
     */
    @GetMapping("/calendars/{calendarId}/events")
    public ResponseEntity<?> listEvents(
            @PathVariable String calendarId,
            @RequestParam(defaultValue = "50") int maxResults) {

        if (!tokenService.hasToken()) {
            return ResponseEntity.status(401).body(
                "No autenticado. Visita primero /google-calendar/auth/google"
            );
        }

        try {
            Calendar service = buildCalendarService();

            Events events = service.events().list(calendarId)
                    .setMaxResults(maxResults)
                    .setSingleEvents(true)   // expande eventos recurrentes en instancias individuales
                    .setOrderBy("startTime") // ordena por fecha de inicio
                    .execute();

            List<Event> items = events.getItems();

            if (items == null || items.isEmpty()) {
                return ResponseEntity.ok("No se encontraron eventos en este calendario.");
            }

            return ResponseEntity.ok(items);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error al listar eventos: " + e.getMessage());
        }
    }




     /**
     * Construye el cliente de Google Calendar usando el Credential guardado en GoogleTokenService.
     */
    private Calendar buildCalendarService() throws Exception {
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                tokenService.getCredential()
        ).setApplicationName(APPLICATION_NAME).build();
    }

}