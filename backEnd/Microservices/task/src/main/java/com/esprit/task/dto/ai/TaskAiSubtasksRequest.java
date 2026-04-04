package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request AI-generated subtask proposals for a task")
public class TaskAiSubtasksRequest {

    @NotNull
    @Schema(description = "Parent task ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @NotNull
    @Schema(description = "Freelancer (assignee) user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long freelancerId;
}
