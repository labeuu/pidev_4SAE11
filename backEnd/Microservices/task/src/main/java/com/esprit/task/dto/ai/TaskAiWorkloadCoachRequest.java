package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for AI workload coaching: narrative guidance from current assignee workload snapshot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workload coach request")
public class TaskAiWorkloadCoachRequest {

    @NotNull
    @Schema(description = "Freelancer (assignee) user id", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long freelancerId;

    /** Horizon in days for due-soon slice included in snapshot; default applied in service if null. */
    @Schema(description = "Due-soon window in days from today (inclusive)", example = "7")
    private Integer horizonDays;
}
