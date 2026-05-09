package tfg.prod;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        try {
            handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(Application.class);
        } catch (ContainerInitializationException e) {
            throw new RuntimeException("No se pudo inicializar Spring Boot en Lambda", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        byte[] bytes = input.readAllBytes();
        JsonNode event = mapper.readTree(bytes);

        // EventBridge scheduled events tienen "source": "aws.events"
        if ("aws.events".equals(event.path("source").asText())) {
            ScheduledRefreshService.getInstance().refreshAll();
            output.write("{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }

        handler.proxyStream(new ByteArrayInputStream(bytes), output, context);
    }
}