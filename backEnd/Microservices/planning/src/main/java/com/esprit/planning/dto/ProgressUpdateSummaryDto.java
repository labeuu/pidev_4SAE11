package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Summary of a progress update for list/pagination responses")
public class ProgressUpdateSummaryDto {

    @Schema(description = "Progress update ID")
    private Long id;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Freelancer ID")
    private Long freelancerId;

    @Schema(description = "Update title")
    private String title;

    @Schema(description = "Progress percentage (0-100)")
    private Integer progressPercentage;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Number of comments on this update")
    private long commentCount;

    public ProgressUpdateSummaryDto() {}

    public ProgressUpdateSummaryDto(Long id, Long projectId, Long freelancerId, String title,
                                    Integer progressPercentage, LocalDateTime createdAt, long commentCount) {
        this.id = id;
        this.projectId = projectId;
        this.freelancerId = freelancerId;
        this.title = title;
        this.progressPercentage = progressPercentage;
        this.createdAt = createdAt;
        this.commentCount = commentCount;
    }

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getFreelancerId() { return freelancerId; }
    public String getTitle() { return title; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getCommentCount() { return commentCount; }

    public void setId(Long id) { this.id = id; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public void setTitle(String title) { this.title = title; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long projectId;
        private Long freelancerId;
        private String title;
        private Integer progressPercentage;
        private LocalDateTime createdAt;
        private long commentCount;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder projectId(Long v) { this.projectId = v; return this; }
        public Builder freelancerId(Long v) { this.freelancerId = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder progressPercentage(Integer v) { this.progressPercentage = v; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }
        public Builder commentCount(long v) { this.commentCount = v; return this; }

        public ProgressUpdateSummaryDto build() {
            return new ProgressUpdateSummaryDto(id, projectId, freelancerId, title,
                    progressPercentage, createdAt, commentCount);
        }
    }
}
