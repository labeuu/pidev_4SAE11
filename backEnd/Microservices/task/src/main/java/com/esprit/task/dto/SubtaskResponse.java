package com.esprit.task.dto;

import com.esprit.task.entity.Subtask;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Subtask returned from API")
public class SubtaskResponse {

    private Long id;
    private Long parentTaskId;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private LocalDate dueDate;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubtaskResponse from(Subtask s) {
        return SubtaskResponse.builder()
                .id(s.getId())
                .parentTaskId(s.getParent() != null ? s.getParent().getId() : null)
                .projectId(s.getProjectId())
                .title(s.getTitle())
                .description(s.getDescription())
                .status(s.getStatus())
                .priority(s.getPriority())
                .assigneeId(s.getAssigneeId())
                .dueDate(s.getDueDate())
                .orderIndex(s.getOrderIndex())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
