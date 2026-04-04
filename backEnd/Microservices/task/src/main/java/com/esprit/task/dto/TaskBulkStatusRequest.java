package com.esprit.task.dto;

import com.esprit.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk update: set the same status on many tasks")
public class TaskBulkStatusRequest {

    @NotEmpty
    @Schema(description = "Task IDs to update", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> taskIds;

    @NotNull
    @Schema(description = "New status for all listed tasks", requiredMode = Schema.RequiredMode.REQUIRED)
    private TaskStatus status;
}
