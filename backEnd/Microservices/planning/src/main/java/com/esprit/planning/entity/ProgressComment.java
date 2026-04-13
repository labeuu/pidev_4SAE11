package com.esprit.planning.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progress_comment")
@Schema(description = "A comment on a progress update")
public class ProgressComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "progress_update_id", nullable = false)
    @JsonIgnore
    private ProgressUpdate progressUpdate;

    @Column(nullable = false)
    @Schema(description = "User ID who wrote the comment", example = "5")
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(description = "Comment text", example = "Great progress!")
    private String message;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    public ProgressComment() {}

    public ProgressComment(Long id, ProgressUpdate progressUpdate, Long userId, String message, LocalDateTime createdAt) {
        this.id = id;
        this.progressUpdate = progressUpdate;
        this.userId = userId;
        this.message = message;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getProgressUpdateId() {
        return progressUpdate != null ? progressUpdate.getId() : null;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public ProgressUpdate getProgressUpdate() { return progressUpdate; }
    public Long getUserId() { return userId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setProgressUpdate(ProgressUpdate progressUpdate) { this.progressUpdate = progressUpdate; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setMessage(String message) { this.message = message; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private ProgressUpdate progressUpdate;
        private Long userId;
        private String message;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder progressUpdate(ProgressUpdate progressUpdate) { this.progressUpdate = progressUpdate; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ProgressComment build() {
            return new ProgressComment(id, progressUpdate, userId, message, createdAt);
        }
    }
}
