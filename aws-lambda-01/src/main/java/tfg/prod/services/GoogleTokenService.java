package tfg.prod.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.auth.oauth2.BearerToken;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenService { // para guardar el token

    private Credential credential;

    public void saveToken(GoogleTokenResponse tokenResponse) {
        this.credential = new Credential(
                BearerToken.authorizationHeaderAccessMethod()
        ).setAccessToken(tokenResponse.getAccessToken())
         .setRefreshToken(tokenResponse.getRefreshToken());
    }

    public Credential getCredential() {
        return credential;
    }

    public boolean hasToken() {
        return credential != null;
    }
}