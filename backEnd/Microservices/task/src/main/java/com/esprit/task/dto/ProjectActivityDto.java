package com.esprit.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Recency and open workload per project for an assignee")
public class ProjectActivityDto {

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Latest activity on any assigned task or its subtasks in this project")
    private LocalDateTime lastActivityAt;

    @Schema(description = "Open root tasks (not DONE/CANCELLED) for this assignee in the project")
    private long openTaskCount;
}
