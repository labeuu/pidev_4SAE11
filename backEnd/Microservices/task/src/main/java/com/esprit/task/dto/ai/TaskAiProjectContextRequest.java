package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request AI-generated task proposals for a project")
public class TaskAiProjectContextRequest {

    @NotNull
    @Schema(description = "Project ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @NotNull
    @Schema(description = "Freelancer user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long freelancerId;
}
