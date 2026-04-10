package com.esprit.planning.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "progress_update")
@Schema(description = "A progress update for a project (submitted by a freelancer)")
public class ProgressUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Column(nullable = true)
    @Schema(description = "Contract ID (null while contract microservice is not available)", example = "1")
    private Long contractId;

    @Column(nullable = false)
    @Schema(description = "Freelancer ID who submitted the update", example = "10")
    private Long freelancerId;

    @Column(nullable = false)
    @Schema(description = "Title of the update", example = "Backend API completed")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Detailed description of progress", example = "Implemented all REST endpoints.")
    private String description;

    @Column(nullable = false)
    @Schema(description = "Progress percentage (0-100)", example = "75")
    private Integer progressPercentage;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    @Schema(description = "Optional: next progress update due date (for reminders / Google Calendar)", example = "2025-03-15T14:00:00")
    private LocalDateTime nextUpdateDue;

    @Column(nullable = true, length = 512)
    @Schema(description = "Google Calendar event ID for the 'next update due' reminder (used to update/delete when nextUpdateDue changes)", accessMode = Schema.AccessMode.READ_ONLY)
    private String nextDueCalendarEventId;

    @Column(nullable = false)
    @Schema(description = "Set true after scheduler notified freelancer that nextUpdateDue passed", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean nextDueOverdueNotified = false;

    @Column(nullable = true, length = 512)
    @Schema(description = "Optional GitHub repository URL (e.g. https://github.com/owner/repo) linked to this progress update")
    private String githubRepoUrl;

    @OneToMany(mappedBy = "progressUpdate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private List<ProgressComment> comments = new ArrayList<>();

    public ProgressUpdate() {}

    public ProgressUpdate(Long id, Long projectId, Long contractId, Long freelancerId,
                          String title, String description, Integer progressPercentage,
                          LocalDateTime createdAt, LocalDateTime updatedAt,
                          LocalDateTime nextUpdateDue, String nextDueCalendarEventId,
                          Boolean nextDueOverdueNotified, String githubRepoUrl,
                          List<ProgressComment> comments) {
        this.id = id;
        this.projectId = projectId;
        this.contractId = contractId;
        this.freelancerId = freelancerId;
        this.title = title;
        this.description = description;
        this.progressPercentage = progressPercentage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.nextUpdateDue = nextUpdateDue;
        this.nextDueCalendarEventId = nextDueCalendarEventId;
        this.nextDueOverdueNotified = nextDueOverdueNotified != null ? nextDueOverdueNotified : false;
        this.githubRepoUrl = githubRepoUrl;
        this.comments = comments != null ? comments : new ArrayList<>();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getContractId() { return contractId; }
    public Long getFreelancerId() { return freelancerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getNextUpdateDue() { return nextUpdateDue; }
    public String getNextDueCalendarEventId() { return nextDueCalendarEventId; }
    public Boolean getNextDueOverdueNotified() { return nextDueOverdueNotified; }
    public String getGithubRepoUrl() { return githubRepoUrl; }
    public List<ProgressComment> getComments() { return comments; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setNextUpdateDue(LocalDateTime nextUpdateDue) { this.nextUpdateDue = nextUpdateDue; }
    public void setNextDueCalendarEventId(String nextDueCalendarEventId) { this.nextDueCalendarEventId = nextDueCalendarEventId; }
    public void setNextDueOverdueNotified(Boolean nextDueOverdueNotified) { this.nextDueOverdueNotified = nextDueOverdueNotified; }
    public void setGithubRepoUrl(String githubRepoUrl) { this.githubRepoUrl = githubRepoUrl; }
    public void setComments(List<ProgressComment> comments) { this.comments = comments; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long projectId;
        private Long contractId;
        private Long freelancerId;
        private String title;
        private String description;
        private Integer progressPercentage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime nextUpdateDue;
        private String nextDueCalendarEventId;
        private Boolean nextDueOverdueNotified = false;
        private String githubRepoUrl;
        private List<ProgressComment> comments = new ArrayList<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder contractId(Long contractId) { this.contractId = contractId; return this; }
        public Builder freelancerId(Long freelancerId) { this.freelancerId = freelancerId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder progressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder nextUpdateDue(LocalDateTime nextUpdateDue) { this.nextUpdateDue = nextUpdateDue; return this; }
        public Builder nextDueCalendarEventId(String nextDueCalendarEventId) { this.nextDueCalendarEventId = nextDueCalendarEventId; return this; }
        public Builder nextDueOverdueNotified(Boolean nextDueOverdueNotified) { this.nextDueOverdueNotified = nextDueOverdueNotified; return this; }
        public Builder githubRepoUrl(String githubRepoUrl) { this.githubRepoUrl = githubRepoUrl; return this; }
        public Builder comments(List<ProgressComment> comments) { this.comments = comments; return this; }

        public ProgressUpdate build() {
            return new ProgressUpdate(id, projectId, contractId, freelancerId, title, description,
                    progressPercentage, createdAt, updatedAt, nextUpdateDue, nextDueCalendarEventId,
                    nextDueOverdueNotified, githubRepoUrl, comments);
        }
    }
}
