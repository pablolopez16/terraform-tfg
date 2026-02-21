package tfg.prod;

import com.fasterxml.jackson.databind.ObjectMapper;
import tfg.prod.modules.GoogleCredentials;

import java.io.InputStream;

public class CredentialsLoader {

    public static GoogleCredentials load() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = CredentialsLoader.class
                    .getClassLoader()
                    .getResourceAsStream("credentials.json");

            if (is == null) {
                throw new RuntimeException("credentials.json no encontrado en resources");
            }

            return mapper.readValue(is, GoogleCredentials.class);

        } catch (Exception e) {
            throw new RuntimeException("Error cargando credentials.json", e);
        }
    }
}