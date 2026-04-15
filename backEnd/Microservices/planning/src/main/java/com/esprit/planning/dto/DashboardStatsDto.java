package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dashboard-wide progress statistics")
public class DashboardStatsDto {

    @Schema(description = "Total number of progress updates", example = "150")
    private long totalUpdates;

    @Schema(description = "Total number of comments on updates", example = "80")
    private long totalComments;

    @Schema(description = "Average progress percentage across all updates", example = "55.5")
    private Double averageProgressPercentage;

    @Schema(description = "Number of distinct projects with updates", example = "25")
    private long distinctProjectCount;

    @Schema(description = "Number of distinct freelancers who submitted updates", example = "40")
    private long distinctFreelancerCount;

    public DashboardStatsDto() {}

    public DashboardStatsDto(long totalUpdates, long totalComments, Double averageProgressPercentage,
                             long distinctProjectCount, long distinctFreelancerCount) {
        this.totalUpdates = totalUpdates;
        this.totalComments = totalComments;
        this.averageProgressPercentage = averageProgressPercentage;
        this.distinctProjectCount = distinctProjectCount;
        this.distinctFreelancerCount = distinctFreelancerCount;
    }

    public long getTotalUpdates() { return totalUpdates; }
    public long getTotalComments() { return totalComments; }
    public Double getAverageProgressPercentage() { return averageProgressPercentage; }
    public long getDistinctProjectCount() { return distinctProjectCount; }
    public long getDistinctFreelancerCount() { return distinctFreelancerCount; }

    public void setTotalUpdates(long totalUpdates) { this.totalUpdates = totalUpdates; }
    public void setTotalComments(long totalComments) { this.totalComments = totalComments; }
    public void setAverageProgressPercentage(Double averageProgressPercentage) { this.averageProgressPercentage = averageProgressPercentage; }
    public void setDistinctProjectCount(long distinctProjectCount) { this.distinctProjectCount = distinctProjectCount; }
    public void setDistinctFreelancerCount(long distinctFreelancerCount) { this.distinctFreelancerCount = distinctFreelancerCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalUpdates;
        private long totalComments;
        private Double averageProgressPercentage;
        private long distinctProjectCount;
        private long distinctFreelancerCount;

        public Builder totalUpdates(long v) { this.totalUpdates = v; return this; }
        public Builder totalComments(long v) { this.totalComments = v; return this; }
        public Builder averageProgressPercentage(Double v) { this.averageProgressPercentage = v; return this; }
        public Builder distinctProjectCount(long v) { this.distinctProjectCount = v; return this; }
        public Builder distinctFreelancerCount(long v) { this.distinctFreelancerCount = v; return this; }

        public DashboardStatsDto build() {
            return new DashboardStatsDto(totalUpdates, totalComments, averageProgressPercentage,
                    distinctProjectCount, distinctFreelancerCount);
        }
    }
}
