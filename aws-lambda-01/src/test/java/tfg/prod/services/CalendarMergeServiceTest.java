package tfg.prod.services;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tfg.prod.modules.MergedEvent;
import tfg.prod.modules.MergeConfig;
import tfg.prod.modules.MergeSource;
import java.io.IOException;
 
import java.util.List;
 
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class CalendarMergeServiceTest {
 
    @Mock
    private GoogleTokenService googleTokenService;
 
    @Mock
    private CalDavSessionService calDavSessionService;
 
    private CalendarMergeService service;
 
    @BeforeEach
    void setUp() {
        service = new CalendarMergeService(googleTokenService, calDavSessionService);
    }
 
    // ---- Tests de extractIcsField ----
 
    @Test
    void extractIcsField_campoSimple() {
        String ics = "BEGIN:VCALENDAR\r\nSUMMARY:Mi evento\r\nEND:VCALENDAR";
        assertEquals("Mi evento", service.extractIcsField(ics, "SUMMARY"));
    }
 
    @Test
    void extractIcsField_campoConParametros() {
        String ics = "BEGIN:VCALENDAR\r\nDTSTART;VALUE=DATE:20260526\r\nEND:VCALENDAR";
        assertEquals("20260526", service.extractIcsField(ics, "DTSTART"));
    }
 
    @Test
    void extractIcsField_campoNoExiste() {
        String ics = "BEGIN:VCALENDAR\r\nSUMMARY:Test\r\nEND:VCALENDAR";
        assertNull(service.extractIcsField(ics, "LOCATION"));
    }
 
    @Test
    void extractIcsField_separadorLF() {
        String ics = "BEGIN:VCALENDAR\nSUMMARY:Evento LF\nEND:VCALENDAR";
        assertEquals("Evento LF", service.extractIcsField(ics, "SUMMARY"));
    }
 
    @Test
    void extractIcsField_entidadesXmlDesescapadas() {
        String icsEscapado = "BEGIN:VCALENDAR&#13;&#10;SUMMARY:Evento iCloud&#13;&#10;DTSTART:20260526T100000&#13;&#10;DTEND:20260526T110000&#13;&#10;UID:test-uid&#13;&#10;END:VCALENDAR";
        String icsLimpio = icsEscapado
            .replace("&#13;&#10;", "\r\n")
            .replace("&#13;", "\r")
            .replace("&#10;", "\n");
        assertEquals("Evento iCloud", service.extractIcsField(icsLimpio, "SUMMARY"));
        assertEquals("20260526T100000", service.extractIcsField(icsLimpio, "DTSTART"));
    }
 
    // ---- Tests de merge con proveedor desconocido ----
 
    @Test
    void merge_proveedorDesconocidoDevuelveListaVacia() {
        MergeSource source = new MergeSource();
        source.setProvider("outlook");
        source.setAccountId("account-1");
        source.setCalendarId("calendar-1");
 
        MergeConfig config = new MergeConfig();
        config.setSources(List.of(source));
        config.setMaxResultsPerCalendar(10);
 
        List<MergedEvent> result = service.merge(config);
        assertTrue(result.isEmpty());
    }
 
    @Test
    void merge_listaFuentesVaciaDevuelveListaVacia() {
        MergeConfig config = new MergeConfig();
        config.setSources(List.of());
        config.setMaxResultsPerCalendar(10);
 
        List<MergedEvent> result = service.merge(config);
        assertTrue(result.isEmpty());
    }
 
    @Test
    void merge_errorEnFuenteNoPropagaExcepcion() throws Exception {
        MergeSource source = new MergeSource();
        source.setProvider("google");
        source.setAccountId("account-error");
        source.setCalendarId("primary");
 
        when(googleTokenService.hasToken("account-error")).thenReturn(false);
        when(googleTokenService.getCredential("account-error")).thenThrow(new IOException("No token"));
 
        MergeConfig config = new MergeConfig();
        config.setSources(List.of(source));
        config.setMaxResultsPerCalendar(10);
 
        List<MergedEvent> result = service.merge(config);
        assertTrue(result.isEmpty());
    }
}
 
