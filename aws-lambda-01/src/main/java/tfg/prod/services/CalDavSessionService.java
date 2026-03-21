package tfg.prod.services;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tfg.prod.services.CalDavSessionService;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
@RestController
@RequestMapping("/caldav")
public class CalDavSessionService {

    private String username;
    private String password;
    private String serverUrl;

    public void saveSession(String username, String password, String serverUrl) {
        this.username  = username;
        this.password  = password;
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }

    public String getUsername()  { return username; }
    public String getPassword()  { return password; }
    public String getServerUrl() { return serverUrl; }

    public boolean hasSession() {
        return username != null && password != null && serverUrl != null;
    }

    public void clearSession() {
        this.username  = null;
        this.password  = null;
        this.serverUrl = null;
    }
}
