package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Calendar event (from Google Calendar when integration is enabled)")
public class CalendarEventDto {

    @Schema(description = "Event ID", example = "abc123")
    private String id;

    @Schema(description = "Event title/summary", example = "Next progress update due – Backend API")
    private String summary;

    @Schema(description = "Start time")
    private LocalDateTime start;

    @Schema(description = "End time")
    private LocalDateTime end;

    @Schema(description = "Optional description")
    private String description;

    public CalendarEventDto() {}

    public CalendarEventDto(String id, String summary, LocalDateTime start, LocalDateTime end, String description) {
        this.id = id;
        this.summary = summary;
        this.start = start;
        this.end = end;
        this.description = description;
    }

    public String getId() { return id; }
    public String getSummary() { return summary; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public String getDescription() { return description; }

    public void setId(String id) { this.id = id; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public void setDescription(String description) { this.description = description; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String summary;
        private LocalDateTime start;
        private LocalDateTime end;
        private String description;

        public Builder id(String id) { this.id = id; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder start(LocalDateTime start) { this.start = start; return this; }
        public Builder end(LocalDateTime end) { this.end = end; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public CalendarEventDto build() {
            return new CalendarEventDto(id, summary, start, end, description);
        }
    }
}
