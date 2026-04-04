package com.esprit.task.dto.ai;

import com.esprit.task.entity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A proposed task or subtask from AI")
public class AiProposedTaskDto {

    private String title;
    private String description;
    @Schema(description = "Mapped from AI low/medium/high")
    private TaskPriority suggestedPriority;
    @Schema(description = "Suggested deadline yyyy-MM-dd from AI; may be null if missing or invalid")
    private LocalDate suggestedDueDate;
}
