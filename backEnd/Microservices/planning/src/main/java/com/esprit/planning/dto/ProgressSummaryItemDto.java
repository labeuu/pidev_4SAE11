package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Lightweight summary item for bulk progress update queries.
 * Used by GET /summary (projectIds or contractIds) and GET /freelancer/{id}/projects-summary.
 */
@Schema(description = "Lightweight progress summary for a project or contract")
public class ProgressSummaryItemDto {

    @Schema(description = "Project ID (present for project or freelancer summary)")
    private Long projectId;

    @Schema(description = "Contract ID (present for contract summary)")
    private Long contractId;

    @Schema(description = "Current progress percentage (0-100)", example = "75")
    private Integer currentProgressPercentage;

    @Schema(description = "Timestamp of the most recent progress update")
    private LocalDateTime lastUpdateAt;

    public ProgressSummaryItemDto() {}

    public ProgressSummaryItemDto(Long projectId, Long contractId,
                                  Integer currentProgressPercentage, LocalDateTime lastUpdateAt) {
        this.projectId = projectId;
        this.contractId = contractId;
        this.currentProgressPercentage = currentProgressPercentage;
        this.lastUpdateAt = lastUpdateAt;
    }

    public Long getProjectId() { return projectId; }
    public Long getContractId() { return contractId; }
    public Integer getCurrentProgressPercentage() { return currentProgressPercentage; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setCurrentProgressPercentage(Integer currentProgressPercentage) { this.currentProgressPercentage = currentProgressPercentage; }
    public void setLastUpdateAt(LocalDateTime lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long projectId;
        private Long contractId;
        private Integer currentProgressPercentage;
        private LocalDateTime lastUpdateAt;

        public Builder projectId(Long v) { this.projectId = v; return this; }
        public Builder contractId(Long v) { this.contractId = v; return this; }
        public Builder currentProgressPercentage(Integer v) { this.currentProgressPercentage = v; return this; }
        public Builder lastUpdateAt(LocalDateTime v) { this.lastUpdateAt = v; return this; }

        public ProgressSummaryItemDto build() {
            return new ProgressSummaryItemDto(projectId, contractId, currentProgressPercentage, lastUpdateAt);
        }
    }
}
