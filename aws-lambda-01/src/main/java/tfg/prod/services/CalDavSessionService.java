package tfg.prod.services;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CalDavSessionService {

    private static class CalDavSession {
        final String username, password, serverUrl;
        CalDavSession(String u, String p, String s) {
            this.username  = u;
            this.password  = p;
            this.serverUrl = s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
        }
    }

    private final Map<String, CalDavSession> sessions = new ConcurrentHashMap<>();
    private final DynamoDbService dynamoDbService;

    public CalDavSessionService(DynamoDbService dynamoDbService) {
        this.dynamoDbService = dynamoDbService;
    }

    public void saveSession(String accountId, String username, String password, String serverUrl) {
        String url = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        sessions.put(accountId, new CalDavSession(username, password, url));
        dynamoDbService.saveCalDavSession(accountId, username, password, url);
    }

    public String getUsername(String accountId)  { CalDavSession s = loadSession(accountId); return s != null ? s.username  : null; }
    public String getPassword(String accountId)  { CalDavSession s = loadSession(accountId); return s != null ? s.password  : null; }
    public String getServerUrl(String accountId) { CalDavSession s = loadSession(accountId); return s != null ? s.serverUrl : null; }

    public boolean hasSession(String accountId) { return loadSession(accountId) != null; }

    private CalDavSession loadSession(String accountId) {
        if (sessions.containsKey(accountId)) return sessions.get(accountId);
        Map<String, String> data = dynamoDbService.getCalDavSession(accountId);
        if (data == null || data.get("username") == null) return null;
        CalDavSession session = new CalDavSession(data.get("username"), data.get("password"), data.get("serverUrl"));
        sessions.put(accountId, session);
        return session;
    }

    public void clearSession(String accountId) {
        sessions.remove(accountId);
        dynamoDbService.deleteAccount(accountId);
    }
}
