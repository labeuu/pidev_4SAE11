package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Freelancer activity ranking (updates and comments on their updates)")
public class FreelancerActivityDto {

    @Schema(description = "Freelancer ID", example = "10")
    private Long freelancerId;

    @Schema(description = "Number of progress updates submitted")
    private long updateCount;

    @Schema(description = "Number of comments on this freelancer's updates")
    private long commentCount;

    public FreelancerActivityDto() {}

    public FreelancerActivityDto(Long freelancerId, long updateCount, long commentCount) {
        this.freelancerId = freelancerId;
        this.updateCount = updateCount;
        this.commentCount = commentCount;
    }

    public Long getFreelancerId() { return freelancerId; }
    public long getUpdateCount() { return updateCount; }
    public long getCommentCount() { return commentCount; }

    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public void setUpdateCount(long updateCount) { this.updateCount = updateCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
}
