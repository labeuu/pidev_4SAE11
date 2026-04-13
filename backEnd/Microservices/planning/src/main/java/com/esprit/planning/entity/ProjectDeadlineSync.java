package com.esprit.planning.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Tracks project deadlines that have been synced to Google Calendar
 * so we do not create duplicate events.
 */
@Entity
@Table(name = "project_deadline_sync", uniqueConstraints = @UniqueConstraint(columnNames = "projectId"))
public class ProjectDeadlineSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long projectId;

    @Column(nullable = false, length = 512)
    private String calendarEventId;

    @Column(nullable = false)
    private LocalDateTime syncedAt;

    public ProjectDeadlineSync() {}

    public ProjectDeadlineSync(Long id, Long projectId, String calendarEventId, LocalDateTime syncedAt) {
        this.id = id;
        this.projectId = projectId;
        this.calendarEventId = calendarEventId;
        this.syncedAt = syncedAt;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getCalendarEventId() { return calendarEventId; }
    public LocalDateTime getSyncedAt() { return syncedAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setCalendarEventId(String calendarEventId) { this.calendarEventId = calendarEventId; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long projectId;
        private String calendarEventId;
        private LocalDateTime syncedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder calendarEventId(String calendarEventId) { this.calendarEventId = calendarEventId; return this; }
        public Builder syncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; return this; }

        public ProjectDeadlineSync build() {
            return new ProjectDeadlineSync(id, projectId, calendarEventId, syncedAt);
        }
    }
}
