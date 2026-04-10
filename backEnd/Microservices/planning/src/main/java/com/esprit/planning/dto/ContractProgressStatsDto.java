package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Progress statistics for a contract")
public class ContractProgressStatsDto {

    @Schema(description = "Contract ID", example = "1")
    private Long contractId;

    @Schema(description = "Number of progress updates", example = "10")
    private long updateCount;

    @Schema(description = "Total comments on this contract's updates", example = "4")
    private long commentCount;

    @Schema(description = "Current progress percentage (latest or max)", example = "75")
    private Integer currentProgressPercentage;

    @Schema(description = "Timestamp of the first update")
    private LocalDateTime firstUpdateAt;

    @Schema(description = "Timestamp of the most recent update")
    private LocalDateTime lastUpdateAt;

    public ContractProgressStatsDto() {}

    public ContractProgressStatsDto(Long contractId, long updateCount, long commentCount,
                                    Integer currentProgressPercentage,
                                    LocalDateTime firstUpdateAt, LocalDateTime lastUpdateAt) {
        this.contractId = contractId;
        this.updateCount = updateCount;
        this.commentCount = commentCount;
        this.currentProgressPercentage = currentProgressPercentage;
        this.firstUpdateAt = firstUpdateAt;
        this.lastUpdateAt = lastUpdateAt;
    }

    public Long getContractId() { return contractId; }
    public long getUpdateCount() { return updateCount; }
    public long getCommentCount() { return commentCount; }
    public Integer getCurrentProgressPercentage() { return currentProgressPercentage; }
    public LocalDateTime getFirstUpdateAt() { return firstUpdateAt; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }

    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setUpdateCount(long updateCount) { this.updateCount = updateCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public void setCurrentProgressPercentage(Integer currentProgressPercentage) { this.currentProgressPercentage = currentProgressPercentage; }
    public void setFirstUpdateAt(LocalDateTime firstUpdateAt) { this.firstUpdateAt = firstUpdateAt; }
    public void setLastUpdateAt(LocalDateTime lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long contractId;
        private long updateCount;
        private long commentCount;
        private Integer currentProgressPercentage;
        private LocalDateTime firstUpdateAt;
        private LocalDateTime lastUpdateAt;

        public Builder contractId(Long contractId) { this.contractId = contractId; return this; }
        public Builder updateCount(long updateCount) { this.updateCount = updateCount; return this; }
        public Builder commentCount(long commentCount) { this.commentCount = commentCount; return this; }
        public Builder currentProgressPercentage(Integer v) { this.currentProgressPercentage = v; return this; }
        public Builder firstUpdateAt(LocalDateTime v) { this.firstUpdateAt = v; return this; }
        public Builder lastUpdateAt(LocalDateTime v) { this.lastUpdateAt = v; return this; }

        public ContractProgressStatsDto build() {
            return new ContractProgressStatsDto(contractId, updateCount, commentCount,
                    currentProgressPercentage, firstUpdateAt, lastUpdateAt);
        }
    }
}
