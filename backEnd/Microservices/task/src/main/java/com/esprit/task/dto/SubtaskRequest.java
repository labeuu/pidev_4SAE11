package com.esprit.task.dto;

import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create or update a subtask")
public class SubtaskRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private Long assigneeId;

    private LocalDate dueDate;

    private Integer orderIndex;
}
