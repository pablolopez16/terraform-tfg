package tfg.prod.services;

import org.springframework.stereotype.Service;
import tfg.prod.modules.MergedEvent;

import java.util.List;

@Service
public class IcsGeneratorService {

    public String generate(List<MergedEvent> events, String calendarName) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//TFG Calendar Merger//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("X-WR-CALNAME:").append(escapeIcs(calendarName)).append("\r\n");

        for (MergedEvent e : events) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(e.getUid()).append("\r\n");
            if (e.isAllDay()) {
                sb.append("DTSTART;VALUE=DATE:").append(e.getStart()).append("\r\n");
                sb.append("DTEND;VALUE=DATE:").append(e.getEnd()).append("\r\n");
            } else {
                sb.append("DTSTART:").append(e.getStart()).append("\r\n");
                sb.append("DTEND:").append(e.getEnd()).append("\r\n");
            }
            sb.append("SUMMARY:").append(escapeIcs(e.getSummary())).append("\r\n");
            if (e.getDescription() != null)
                sb.append("DESCRIPTION:").append(escapeIcs(e.getDescription())).append("\r\n");
            if (e.getLocation() != null)
                sb.append("LOCATION:").append(escapeIcs(e.getLocation())).append("\r\n");
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private String escapeIcs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace(",",  "\\,")
                   .replace(";",  "\\;")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }
}