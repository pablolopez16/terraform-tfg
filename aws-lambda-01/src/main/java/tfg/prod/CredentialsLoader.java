package tfg.prod;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import tfg.prod.modules.GoogleCredentials;

public class CredentialsLoader {

    public static GoogleCredentials load() {
        try {
            String secretArn = System.getenv("GOOGLE_CREDENTIALS_SECRET_ARN");
            if (secretArn == null || secretArn.isBlank()) {
                throw new RuntimeException("Variable de entorno GOOGLE_CREDENTIALS_SECRET_ARN no definida");
            }

            SecretsManagerClient client = SecretsManagerClient.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build();

            GetSecretValueResponse response = client.getSecretValue(
                    GetSecretValueRequest.builder().secretId(secretArn).build()
            );

            return new ObjectMapper().readValue(response.secretString(), GoogleCredentials.class);

        } catch (Exception e) {
            throw new RuntimeException("Error cargando credenciales de Google desde Secrets Manager", e);
        }
    }
}