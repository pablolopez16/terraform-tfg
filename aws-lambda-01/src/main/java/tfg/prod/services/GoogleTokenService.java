package tfg.prod.services;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.stereotype.Service;
import tfg.prod.CredentialsLoader;
import tfg.prod.modules.GoogleCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleTokenService {

    private static final String TOKEN_SERVER_URL = "https://oauth2.googleapis.com/token";
    private final Map<String, Credential> credentials = new ConcurrentHashMap<>();
    private final DynamoDbService dynamoDbService;

    public GoogleTokenService(DynamoDbService dynamoDbService) {
        this.dynamoDbService = dynamoDbService;
    }

    public void saveToken(String accountId, GoogleTokenResponse tokenResponse) throws GeneralSecurityException, IOException {
        Credential credential = buildCredential()
                .setAccessToken(tokenResponse.getAccessToken())
                .setRefreshToken(tokenResponse.getRefreshToken())
                .setExpiresInSeconds(tokenResponse.getExpiresInSeconds());
        credentials.put(accountId, credential);
        if (tokenResponse.getRefreshToken() != null) {
            dynamoDbService.saveGoogleToken(accountId, tokenResponse.getRefreshToken());
        }
    }

    public String initFromRefreshToken(String accountId, String refreshToken) throws GeneralSecurityException, IOException {
        String tokenToUse = refreshToken != null ? refreshToken : dynamoDbService.getRefreshToken(accountId);
        if (tokenToUse == null) {
            throw new IllegalStateException("No hay refresh_token para la cuenta: " + accountId);
        }
        Credential credential = buildCredential().setRefreshToken(tokenToUse);
        credential.refreshToken();
        credentials.put(accountId, credential);
        dynamoDbService.saveGoogleToken(accountId, tokenToUse);
        return credential.getAccessToken();
    }

    public Credential getCredential(String accountId) throws GeneralSecurityException, IOException {
        if (credentials.containsKey(accountId)) return credentials.get(accountId);
        initFromRefreshToken(accountId, null);
        return credentials.get(accountId);
    }

    public boolean hasToken(String accountId) {
        return credentials.containsKey(accountId) || dynamoDbService.getRefreshToken(accountId) != null;
    }

    private Credential buildCredential() throws GeneralSecurityException, IOException {
        GoogleCredentials creds = CredentialsLoader.load();
        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setTokenServerUrl(new GenericUrl(TOKEN_SERVER_URL))
                .setClientAuthentication(new ClientParametersAuthentication(
                        creds.getClient_id(),
                        creds.getClient_secret()
                ))
                .build();
    }
}
