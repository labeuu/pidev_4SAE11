package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Project with no recent progress update (stalled)")
public class StalledProjectDto {

    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Schema(description = "Timestamp of the last progress update")
    private LocalDateTime lastUpdateAt;

    @Schema(description = "Progress percentage at the last update (0-100)", example = "45")
    private Integer lastProgressPercentage;

    public StalledProjectDto() {}

    public StalledProjectDto(Long projectId, LocalDateTime lastUpdateAt, Integer lastProgressPercentage) {
        this.projectId = projectId;
        this.lastUpdateAt = lastUpdateAt;
        this.lastProgressPercentage = lastProgressPercentage;
    }

    public Long getProjectId() { return projectId; }
    public LocalDateTime getLastUpdateAt() { return lastUpdateAt; }
    public Integer getLastProgressPercentage() { return lastProgressPercentage; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setLastUpdateAt(LocalDateTime lastUpdateAt) { this.lastUpdateAt = lastUpdateAt; }
    public void setLastProgressPercentage(Integer lastProgressPercentage) { this.lastProgressPercentage = lastProgressPercentage; }
}
