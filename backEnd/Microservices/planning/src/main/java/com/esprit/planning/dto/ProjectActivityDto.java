package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project activity ranking by update count")
public class ProjectActivityDto {

    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Schema(description = "Number of progress updates for this project")
    private long updateCount;

    public ProjectActivityDto() {}

    public ProjectActivityDto(Long projectId, long updateCount) {
        this.projectId = projectId;
        this.updateCount = updateCount;
    }

    public Long getProjectId() { return projectId; }
    public long getUpdateCount() { return updateCount; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setUpdateCount(long updateCount) { this.updateCount = updateCount; }
}
