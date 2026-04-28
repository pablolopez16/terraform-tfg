package tfg.prod.modules;

public class MergedEvent {
    private String uid;
    private String summary;
    private String description;
    private String location;
    private String start; // formato ICS: "20240115T100000Z" o "20240115"
    private String end;
    private boolean allDay;

    public MergedEvent(String uid, String summary, String description,
                       String location, String start, String end, boolean allDay) {
        this.uid = uid; this.summary = summary; this.description = description;
        this.location = location; this.start = start; this.end = end; this.allDay = allDay;
    }

    public String getUid()         { return uid; }
    public String getSummary()     { return summary; }
    public String getDescription() { return description; }
    public String getLocation()    { return location; }
    public String getStart()       { return start; }
    public String getEnd()         { return end; }
    public boolean isAllDay()      { return allDay; }
}