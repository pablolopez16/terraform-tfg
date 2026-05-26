package tfg.prod.services;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tfg.prod.modules.MergedEvent;
 
import java.util.List;
 
import static org.junit.jupiter.api.Assertions.*;
 
class IcsGeneratorServiceTest {
 
    private IcsGeneratorService service;
 
    @BeforeEach
    void setUp() {
        service = new IcsGeneratorService();
    }
 
    @Test
    void generate_icsContieneEncabezadoCorrecto() {
        String ics = service.generate(List.of(), "Mi Calendario");
        assertTrue(ics.contains("BEGIN:VCALENDAR"));
        assertTrue(ics.contains("VERSION:2.0"));
        assertTrue(ics.contains("PRODID:-//TFG Calendar Merger//EN"));
        assertTrue(ics.contains("X-WR-CALNAME:Mi Calendario"));
        assertTrue(ics.contains("END:VCALENDAR"));
    }
 
    @Test
    void generate_listaVaciaDevuelveCalendarioSinEventos() {
        String ics = service.generate(List.of(), "Vacío");
        assertFalse(ics.contains("BEGIN:VEVENT"));
    }
 
    @Test
    void generate_eventoNormalConFechaYHora() {
        MergedEvent evento = new MergedEvent(
                "uid-001", "Reunión de trabajo", "Descripción", "Madrid",
                "20260526T100000", "20260526T110000", false
        );
        String ics = service.generate(List.of(evento), "Test");
        assertTrue(ics.contains("BEGIN:VEVENT"));
        assertTrue(ics.contains("UID:uid-001"));
        assertTrue(ics.contains("SUMMARY:Reunión de trabajo"));
        assertTrue(ics.contains("DTSTART:20260526T100000"));
        assertTrue(ics.contains("DTEND:20260526T110000"));
        assertTrue(ics.contains("DESCRIPTION:Descripción"));
        assertTrue(ics.contains("LOCATION:Madrid"));
        assertTrue(ics.contains("END:VEVENT"));
    }
 
    @Test
    void generate_eventoTodoElDiaUsaValueDate() {
        MergedEvent evento = new MergedEvent(
                "uid-002", "Día libre", null, null,
                "20260526", "20260527", true
        );
        String ics = service.generate(List.of(evento), "Test");
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20260526"));
        assertTrue(ics.contains("DTEND;VALUE=DATE:20260527"));
    }
 
    @Test
    void generate_varioseventosEnMismoIcs() {
        MergedEvent e1 = new MergedEvent("uid-001", "[Apple] Médico", null, null, "20260526T090000", "20260526T100000", false);
        MergedEvent e2 = new MergedEvent("uid-002", "[Google] Reunión", null, null, "20260527T110000", "20260527T120000", false);
        String ics = service.generate(List.of(e1, e2), "Fusionado");
        assertTrue(ics.contains("SUMMARY:[Apple] Médico"));
        assertTrue(ics.contains("SUMMARY:[Google] Reunión"));
        assertEquals(2, countOccurrences(ics, "BEGIN:VEVENT"));
    }
 
    @Test
    void generate_caractereEspecialesSeEscapan() {
        MergedEvent evento = new MergedEvent(
                "uid-003", "Evento, con; comas", "Desc\ncon salto", null,
                "20260526T100000", "20260526T110000", false
        );
        String ics = service.generate(List.of(evento), "Test");
        assertTrue(ics.contains("SUMMARY:Evento\\, con\\; comas"));
        assertTrue(ics.contains("DESCRIPTION:Desc\\ncon salto"));
    }
 
    @Test
    void generate_sinDescripcionNiLocalizacion() {
        MergedEvent evento = new MergedEvent("uid-004", "Simple", null, null, "20260526T100000", "20260526T110000", false);
        String ics = service.generate(List.of(evento), "Test");
        assertFalse(ics.contains("DESCRIPTION:"));
        assertFalse(ics.contains("LOCATION:"));
    }
 
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
 