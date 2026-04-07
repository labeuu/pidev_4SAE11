package com.esprit.task.dto;

import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for creating or updating a task")
public class TaskRequest {

    @NotNull(message = "projectId is required")
    @Schema(description = "Project ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @Schema(description = "Contract ID")
    private Long contractId;

    @NotBlank(message = "title is required")
    @Schema(description = "Task title", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Task description")
    private String description;

    @Schema(description = "Task status")
    private TaskStatus status;

    @Schema(description = "Task priority")
    private TaskPriority priority;

    @Schema(description = "Assignee (freelancer) ID")
    private Long assigneeId;

    @Schema(description = "Task deadline for calendar integration")
    private LocalDate dueDate;

    @Schema(description = "Sort order index")
    private Integer orderIndex;

    @Schema(description = "Creator user ID")
    private Long createdBy;
}
