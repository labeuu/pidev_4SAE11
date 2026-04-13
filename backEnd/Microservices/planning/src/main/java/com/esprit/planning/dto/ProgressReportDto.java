package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Time-bounded progress report for a project")
public class ProgressReportDto {

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Report start date (inclusive)")
    private LocalDate from;

    @Schema(description = "Report end date (inclusive)")
    private LocalDate to;

    @Schema(description = "Number of progress updates in the period")
    private long updateCount;

    @Schema(description = "Total number of comments on progress updates in the period")
    private long commentCount;

    @Schema(description = "Average progress percentage over the period")
    private Double averageProgressPercentage;

    @Schema(description = "Timestamp of the first update in the period")
    private LocalDateTime firstUpdateAt;

    @Schema(description = "Timestamp of the last update in the period")
    private LocalDateTime lastUpdateAt;

    public ProgressReportDto() {}

    public ProgressReportDto(Long projectId, LocalDate from, LocalDate to, long updateCount,
                             long commentCount, Double averageProgressPercentage,
                             LocalDateTime firstUpdateAt, LocalDateTime lastUpdateAt) {
        this.projectId = projectId;
        this.from = from;
        this.to = to;
        this.updateCount = updateCount;
        this.commentCount = commentCount;
        this.averageProgressPercentage = averageProgressPercentage;
        this.firstUpdateAt = firstUpdateAt;
        this.lastUpdateAt = lastUpdateAt;
    }

    public Long getProjectId() { return projectId; }
    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public long getUpdateCount() { return updateCount; }
    public long getCommentCount() { return commentCount; }
    public Double getAverageProgressPercentage() { return averageProgressPercentage; }
    public LocalDateTime getFirstUpdateAt() { return firstUpdateAt; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setFrom(LocalDate from) { this.from = from; }
    public void setTo(LocalDate to) { this.to = to; }
    public void setUpdateCount(long updateCount) { this.updateCount = updateCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public void setAverageProgressPercentage(Double averageProgressPercentage) { this.averageProgressPercentage = averageProgressPercentage; }
    public void setFirstUpdateAt(LocalDateTime firstUpdateAt) { this.firstUpdateAt = firstUpdateAt; }
    public void setLastUpdateAt(LocalDateTime lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long projectId;
        private LocalDate from;
        private LocalDate to;
        private long updateCount;
        private long commentCount;
        private Double averageProgressPercentage;
        private LocalDateTime firstUpdateAt;
        private LocalDateTime lastUpdateAt;

        public Builder projectId(Long v) { this.projectId = v; return this; }
        public Builder from(LocalDate v) { this.from = v; return this; }
        public Builder to(LocalDate v) { this.to = v; return this; }
        public Builder updateCount(long v) { this.updateCount = v; return this; }
        public Builder commentCount(long v) { this.commentCount = v; return this; }
        public Builder averageProgressPercentage(Double v) { this.averageProgressPercentage = v; return this; }
        public Builder firstUpdateAt(LocalDateTime v) { this.firstUpdateAt = v; return this; }
        public Builder lastUpdateAt(LocalDateTime v) { this.lastUpdateAt = v; return this; }

        public ProgressReportDto build() {
            return new ProgressReportDto(projectId, from, to, updateCount, commentCount,
                    averageProgressPercentage, firstUpdateAt, lastUpdateAt);
        }
    }
}
