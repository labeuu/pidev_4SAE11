package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Progress statistics for a project")
public class ProjectProgressStatsDto {

    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Schema(description = "Number of progress updates", example = "15")
    private long updateCount;

    @Schema(description = "Total comments on this project's updates", example = "8")
    private long commentCount;

    @Schema(description = "Current progress percentage (latest or max)", example = "80")
    private Integer currentProgressPercentage;

    @Schema(description = "Timestamp of the first update")
    private LocalDateTime firstUpdateAt;

    @Schema(description = "Timestamp of the most recent update")
    private LocalDateTime lastUpdateAt;

    public ProjectProgressStatsDto() {}

    public ProjectProgressStatsDto(Long projectId, long updateCount, long commentCount,
                                   Integer currentProgressPercentage,
                                   LocalDateTime firstUpdateAt, LocalDateTime lastUpdateAt) {
        this.projectId = projectId;
        this.updateCount = updateCount;
        this.commentCount = commentCount;
        this.currentProgressPercentage = currentProgressPercentage;
        this.firstUpdateAt = firstUpdateAt;
        this.lastUpdateAt = lastUpdateAt;
    }

    public Long getProjectId() { return projectId; }
    public long getUpdateCount() { return updateCount; }
    public long getCommentCount() { return commentCount; }
    public Integer getCurrentProgressPercentage() { return currentProgressPercentage; }
    public LocalDateTime getFirstUpdateAt() { return firstUpdateAt; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setUpdateCount(long updateCount) { this.updateCount = updateCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public void setCurrentProgressPercentage(Integer currentProgressPercentage) { this.currentProgressPercentage = currentProgressPercentage; }
    public void setFirstUpdateAt(LocalDateTime firstUpdateAt) { this.firstUpdateAt = firstUpdateAt; }
    public void setLastUpdateAt(LocalDateTime lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long projectId;
        private long updateCount;
        private long commentCount;
        private Integer currentProgressPercentage;
        private LocalDateTime firstUpdateAt;
        private LocalDateTime lastUpdateAt;

        public Builder projectId(Long v) { this.projectId = v; return this; }
        public Builder updateCount(long v) { this.updateCount = v; return this; }
        public Builder commentCount(long v) { this.commentCount = v; return this; }
        public Builder currentProgressPercentage(Integer v) { this.currentProgressPercentage = v; return this; }
        public Builder firstUpdateAt(LocalDateTime v) { this.firstUpdateAt = v; return this; }
        public Builder lastUpdateAt(LocalDateTime v) { this.lastUpdateAt = v; return this; }

        public ProjectProgressStatsDto build() {
            return new ProjectProgressStatsDto(projectId, updateCount, commentCount,
                    currentProgressPercentage, firstUpdateAt, lastUpdateAt);
        }
    }
}
