package com.esprit.task.dto.planning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Minimal JSON view of a Planning MS {@code ProgressUpdate} for Feign deserialization
 * when Task MS aggregates progress history for AI client briefs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningProgressUpdateDto {

    private Long id;
    private Long projectId;
    private Long freelancerId;
    private String title;
    private String description;
    private Integer progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime nextUpdateDue;
}
