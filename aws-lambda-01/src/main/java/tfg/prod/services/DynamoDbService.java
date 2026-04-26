package tfg.prod.services;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class DynamoDbService {

    private final DynamoDbClient client;
    private final String tableName;

    public DynamoDbService() {
        this.client = DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        this.tableName = System.getenv("DYNAMODB_TABLE");
    }

    public void saveGoogleToken(String accountId, String refreshToken) {
        client.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "account_id",   AttributeValue.fromS(accountId),
                        "provider",     AttributeValue.fromS("google"),
                        "refresh_token", AttributeValue.fromS(refreshToken),
                        "updated_at",   AttributeValue.fromS(Instant.now().toString())
                ))
                .build());
    }

    public String getRefreshToken(String accountId) {
        GetItemResponse response = client.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("account_id", AttributeValue.fromS(accountId)))
                .build());
        if (!response.hasItem()) return null;
        AttributeValue token = response.item().get("refresh_token");
        return token != null ? token.s() : null;
    }

    public void saveCalDavSession(String accountId, String username, String password, String serverUrl) {
        client.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "account_id",       AttributeValue.fromS(accountId),
                        "provider",         AttributeValue.fromS("caldav"),
                        "caldav_username",  AttributeValue.fromS(username),
                        "caldav_password",  AttributeValue.fromS(password),
                        "caldav_server_url", AttributeValue.fromS(serverUrl),
                        "updated_at",       AttributeValue.fromS(Instant.now().toString())
                ))
                .build());
    }

    public Map<String, String> getCalDavSession(String accountId) {
        GetItemResponse response = client.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("account_id", AttributeValue.fromS(accountId)))
                .build());
        if (!response.hasItem()) return null;
        Map<String, AttributeValue> item = response.item();
        if (!"caldav".equals(item.getOrDefault("provider", AttributeValue.fromS("")).s())) return null;

        Map<String, String> session = new HashMap<>();
        if (item.containsKey("caldav_username"))   session.put("username",   item.get("caldav_username").s());
        if (item.containsKey("caldav_password"))   session.put("password",   item.get("caldav_password").s());
        if (item.containsKey("caldav_server_url")) session.put("serverUrl",  item.get("caldav_server_url").s());
        return session;
    }

    public void deleteAccount(String accountId) {
        client.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("account_id", AttributeValue.fromS(accountId)))
                .build());
    }
}
