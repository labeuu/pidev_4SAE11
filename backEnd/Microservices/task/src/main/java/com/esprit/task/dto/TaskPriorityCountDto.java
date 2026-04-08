package com.esprit.task.dto;

import com.esprit.task.entity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Number of tasks and subtasks for a priority level")
public class TaskPriorityCountDto {

    @Schema(description = "Priority level")
    private TaskPriority priority;

    @Schema(description = "Count of root tasks and subtasks with this priority")
    private long count;
}
