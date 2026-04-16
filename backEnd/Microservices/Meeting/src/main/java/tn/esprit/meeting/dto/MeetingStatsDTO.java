package tn.esprit.meeting.dto;

public class MeetingStatsDTO {
    private long total;
    private long pending;
    private long accepted;
    private long declined;
    private long cancelled;
    private long completed;

    public long getTotal()     { return total; }
    public long getPending()   { return pending; }
    public long getAccepted()  { return accepted; }
    public long getDeclined()  { return declined; }
    public long getCancelled() { return cancelled; }
    public long getCompleted() { return completed; }

    public void setTotal(long total)         { this.total = total; }
    public void setPending(long pending)     { this.pending = pending; }
    public void setAccepted(long accepted)   { this.accepted = accepted; }
    public void setDeclined(long declined)   { this.declined = declined; }
    public void setCancelled(long cancelled) { this.cancelled = cancelled; }
    public void setCompleted(long completed) { this.completed = completed; }
}
