package com.esprit.task.dto.planning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mirrors Planning {@code ProjectProgressStatsDto} for Feign — used in AI client status brief context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningProjectProgressStatsDto {

    private Long projectId;
    private long updateCount;
    private long commentCount;
    private Integer currentProgressPercentage;
    private LocalDateTime firstUpdateAt;
    private LocalDateTime lastUpdateAt;
}
