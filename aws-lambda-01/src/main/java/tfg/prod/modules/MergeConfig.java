package tfg.prod.modules;

import java.util.List;

public class MergeConfig {
    private String mergeId;
    private List<MergeSource> sources;
    private int maxResultsPerCalendar = 50;

    public String getMergeId()                  { return mergeId; }
    public List<MergeSource> getSources()        { return sources; }
    public int getMaxResultsPerCalendar()        { return maxResultsPerCalendar; }
    public void setMergeId(String v)             { this.mergeId = v; }
    public void setSources(List<MergeSource> v)  { this.sources = v; }
    public void setMaxResultsPerCalendar(int v)  { this.maxResultsPerCalendar = v; }
}
