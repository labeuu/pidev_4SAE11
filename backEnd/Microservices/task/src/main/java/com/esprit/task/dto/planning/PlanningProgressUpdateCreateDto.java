package com.esprit.task.dto.planning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request body for Planning {@code POST /api/progress-updates} — mirrors
 * {@code com.esprit.planning.dto.ProgressUpdateRequest} for Feign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningProgressUpdateCreateDto {

    private Long projectId;
    private Long contractId;
    private Long freelancerId;
    private String title;
    private String description;
    private Integer progressPercentage;
    private LocalDateTime nextUpdateDue;
    private String githubRepoUrl;
}
