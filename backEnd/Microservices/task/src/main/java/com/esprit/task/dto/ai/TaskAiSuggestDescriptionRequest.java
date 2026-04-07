package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request AI description suggestion for a task title")
public class TaskAiSuggestDescriptionRequest {

    @NotNull
    @Schema(description = "Project ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @NotNull
    @Schema(description = "Freelancer (assignee) user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long freelancerId;

    @NotBlank
    @Schema(description = "Task title", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
}
