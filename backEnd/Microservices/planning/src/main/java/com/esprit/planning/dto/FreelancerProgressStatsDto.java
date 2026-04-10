package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Progress statistics for a freelancer")
public class FreelancerProgressStatsDto {

    @Schema(description = "Freelancer ID", example = "10")
    private Long freelancerId;

    @Schema(description = "Total number of progress updates", example = "25")
    private long totalUpdates;

    @Schema(description = "Total comments on this freelancer's updates", example = "12")
    private long totalComments;

    @Schema(description = "Average progress percentage across all updates", example = "62.5")
    private Double averageProgressPercentage;

    @Schema(description = "Timestamp of the most recent update")
    private LocalDateTime lastUpdateAt;

    @Schema(description = "Number of updates in the last 30 days (activity score)", example = "5")
    private long updatesLast30Days;

    public FreelancerProgressStatsDto() {}

    public FreelancerProgressStatsDto(Long freelancerId, long totalUpdates, long totalComments,
                                      Double averageProgressPercentage, LocalDateTime lastUpdateAt,
                                      long updatesLast30Days) {
        this.freelancerId = freelancerId;
        this.totalUpdates = totalUpdates;
        this.totalComments = totalComments;
        this.averageProgressPercentage = averageProgressPercentage;
        this.lastUpdateAt = lastUpdateAt;
        this.updatesLast30Days = updatesLast30Days;
    }

    public Long getFreelancerId() { return freelancerId; }
    public long getTotalUpdates() { return totalUpdates; }
    public long getTotalComments() { return totalComments; }
    public Double getAverageProgressPercentage() { return averageProgressPercentage; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }
    public long getUpdatesLast30Days() { return updatesLast30Days; }

    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public void setTotalUpdates(long totalUpdates) { this.totalUpdates = totalUpdates; }
    public void setTotalComments(long totalComments) { this.totalComments = totalComments; }
    public void setAverageProgressPercentage(Double averageProgressPercentage) { this.averageProgressPercentage = averageProgressPercentage; }
    public void setLastUpdateAt(LocalDateTime lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }
    public void setUpdatesLast30Days(long updatesLast30Days) { this.updatesLast30Days = updatesLast30Days; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long freelancerId;
        private long totalUpdates;
        private long totalComments;
        private Double averageProgressPercentage;
        private LocalDateTime lastUpdateAt;
        private long updatesLast30Days;

        public Builder freelancerId(Long v) { this.freelancerId = v; return this; }
        public Builder totalUpdates(long v) { this.totalUpdates = v; return this; }
        public Builder totalComments(long v) { this.totalComments = v; return this; }
        public Builder averageProgressPercentage(Double v) { this.averageProgressPercentage = v; return this; }
        public Builder lastUpdateAt(LocalDateTime v) { this.lastUpdateAt = v; return this; }
        public Builder updatesLast30Days(long v) { this.updatesLast30Days = v; return this; }

        public FreelancerProgressStatsDto build() {
            return new FreelancerProgressStatsDto(freelancerId, totalUpdates, totalComments,
                    averageProgressPercentage, lastUpdateAt, updatesLast30Days);
        }
    }
}
