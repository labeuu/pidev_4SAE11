package com.esprit.task.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A root task within a project (subtasks live in the subtask table)")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @Column
    @Schema(description = "Contract ID (nullable)", example = "1")
    private Long contractId;

    @Column(nullable = false)
    @Schema(description = "Task title", example = "Implement API")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Task description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Task status")
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Task priority")
    private TaskPriority priority;

    @Column
    @Schema(description = "Assignee (freelancer) ID", example = "10")
    private Long assigneeId;

    @Column
    @Schema(description = "Task deadline — used for Planning calendar integration")
    private LocalDate dueDate;

    @Column(nullable = false)
    @Schema(description = "Sort order index")
    private Integer orderIndex;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Subtask> subtasks = new ArrayList<>();

    /**
     * When true, {@link #id} refers to a row in {@code subtask} (use subtask APIs), not {@code task}.
     * Only set on synthetic views returned by overdue / due-soon style endpoints.
     */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean subtask;

    /**
     * Root task id when {@link #subtask} is true; for API consumers editing synthetic overdue/due-soon rows.
     */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Parent root task id when subtask is true (synthetic views only)", accessMode = Schema.AccessMode.READ_ONLY)
    private Long parentTaskId;

    @Column
    @Schema(description = "Creator user ID")
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
