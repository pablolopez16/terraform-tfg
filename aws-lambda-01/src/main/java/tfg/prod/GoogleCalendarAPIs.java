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
import tfg.prod.*;
import tfg.prod.controller.GoogleCalendarController;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("https://www.googleapis.com/calendar/v3")
public class GoogleCalendarAPIs {
    GoogleCalendarController controller = new GoogleCalendarController();

@GetMapping("/users/me/calendarList")
public ResponseEntity<String> calendarList() {
    ResponseEntity<String> urlAuthentication = controller.redirectToGoogle();
    return responseEntity.ok();


 
}

   

    
}
