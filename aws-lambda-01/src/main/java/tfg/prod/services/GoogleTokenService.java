package tfg.prod.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.gson.GsonFactory;

import tfg.prod.CredentialsLoader;
import tfg.prod.modules.GoogleCredentials;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.stereotype.Service;

@Service
public class GoogleTokenService { // para guardar el token

    private Credential credential;
     private static final String TOKEN_SERVER_URL = "https://oauth2.googleapis.com/token";

    public void saveToken(GoogleTokenResponse tokenResponse) throws GeneralSecurityException, IOException {
        GoogleCredentials creds = CredentialsLoader.load();
 
        // En google-api-client 2.x, Credential OBLIGATORIAMENTE necesita el Builder
        // con transport, jsonFactory, tokenServerUrl y clientAuthentication
        this.credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setTokenServerUrl(new GenericUrl(TOKEN_SERVER_URL))
                .setClientAuthentication(new ClientParametersAuthentication(
                        creds.getClient_id(),
                        creds.getClient_secret()
                ))
                .build()
                .setAccessToken(tokenResponse.getAccessToken())
                .setRefreshToken(tokenResponse.getRefreshToken())
                .setExpiresInSeconds(tokenResponse.getExpiresInSeconds());
    }

    public Credential getCredential() {
        return credential;
    }

    public boolean hasToken() {
        return credential != null;
    }
}