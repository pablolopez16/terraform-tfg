package tfg.prod;

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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import tfg.prod.modules.*;

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
        // Construimos el flujo OAuth
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                creds.getClient_id(),   
                creds.getClient_secret(),
                creds.getScopes()
        ).setAccessType("offline").build();
        System.out.println("Client ID: " + creds.getClient_id());
        System.out.println("Client Secret: " + creds.getClient_secret());
        System.out.println("Redirect URI: " + creds.getRedirect_uri());

        // Intercambiamos el código por tokens
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(creds.getRedirect_uri())
                .execute();

        // Obtenemos el access token y refresh token
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        return ResponseEntity.ok(
                "Access Token: " + accessToken + "\n" +
                "Refresh Token: " + refreshToken
        );

    } catch (TokenResponseException e) {
        // ← Este catch te dirá exactamente qué falla (redirect_uri_mismatch, invalid_client, etc.)
        System.out.println("=== Token Error ===");
        System.out.println("Detalle: " + e.getDetails());
        System.out.println("Mensaje: " + e.getMessage());
        return ResponseEntity.status(500).body("Error detalle: " + e.getDetails());

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("Error al obtener el token: " + e.getMessage());
    }
}



}