package com.esprit.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subtask completion counts for a root task (non-cancelled subtasks only)")
public class SubtaskProgressDto {

    @Schema(description = "Root task ID")
    private Long parentTaskId;

    @Schema(description = "Non-cancelled subtask count")
    private long total;

    @Schema(description = "Subtasks in DONE status")
    private long completed;
}
