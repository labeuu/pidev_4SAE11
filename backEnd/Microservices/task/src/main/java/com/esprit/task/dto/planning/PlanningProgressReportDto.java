package com.esprit.task.dto.planning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mirrors Planning {@code ProgressReportDto} for optional date-bounded report segments in briefs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningProgressReportDto {

    private Long projectId;
    private LocalDate from;
    private LocalDate to;
    private long updateCount;
    private long commentCount;
    private Double averageProgressPercentage;
    private LocalDateTime firstUpdateAt;
    private LocalDateTime lastUpdateAt;
}
