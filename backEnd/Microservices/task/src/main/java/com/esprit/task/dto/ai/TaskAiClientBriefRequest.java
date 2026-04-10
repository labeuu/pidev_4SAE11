package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Client-facing status brief: verifies {@code clientUserId} owns the project, then merges tasks + Planning data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI stakeholder brief request")
public class TaskAiClientBriefRequest {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    /** Must match {@link com.esprit.task.dto.ProjectDto#getClientId()} for the project. */
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientUserId;

    @Schema(description = "Inclusive report start for Planning stats/report (optional)")
    private LocalDate reportFrom;

    @Schema(description = "Inclusive report end for Planning stats/report (optional)")
    private LocalDate reportTo;
}
