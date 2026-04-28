package tfg.prod.modules;

public class MergeSource {
    private String accountId;
    private String provider;   // "google" | "caldav"
    private String calendarId; // Google calendarId o path CalDAV
    private String prefix;     // opcional, p.ej. "[Trabajo] "

    public String getAccountId()  { return accountId; }
    public String getProvider()   { return provider; }
    public String getCalendarId() { return calendarId; }
    public String getPrefix()     { return prefix; }
    public void setAccountId(String v)  { this.accountId = v; }
    public void setProvider(String v)   { this.provider = v; }
    public void setCalendarId(String v) { this.calendarId = v; }
    public void setPrefix(String v)     { this.prefix = v; }
}