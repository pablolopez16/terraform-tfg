package tfg.prod.modules;

import java.util.List;

public class MergeSource {
    private String accountId;
    private String provider;   // "google" | "caldav"
    private String calendarId; // Google calendarId o path CalDAV
    private String prefix;     // opcional, p.ej. "[Trabajo] "
    private List<String> excludeKeywords; // palabras clave a excluir en el título
    private String suffix;                // sufijo opcional

    public String getAccountId()  { return accountId; }
    public String getProvider()   { return provider; }
    public String getCalendarId() { return calendarId; }
    public String getPrefix()     { return prefix; }
    public void setAccountId(String v)  { this.accountId = v; }
    public void setProvider(String v)   { this.provider = v; }
    public void setCalendarId(String v) { this.calendarId = v; }
    public void setPrefix(String v)     { this.prefix = v; }
    public List<String> getExcludeKeywords() { return excludeKeywords; }
    public String getSuffix()               { return suffix; }
    public void setExcludeKeywords(List<String> v) { this.excludeKeywords = v; }
    public void setSuffix(String v)              { this.suffix = v; }
}