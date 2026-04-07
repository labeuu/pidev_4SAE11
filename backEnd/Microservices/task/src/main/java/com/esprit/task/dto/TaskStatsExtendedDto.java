package com.esprit.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Extended task statistics (root tasks + subtasks). "
        + "inProgressCount counts IN_PROGRESS only; use inReviewCount for IN_REVIEW. "
        + "completedInRangeCount counts items in DONE status whose updatedAt fell in the interval (approximates completions without an audit log).")
public class TaskStatsExtendedDto {

    @Schema(description = "Total task and subtask count")
    private long totalTasks;

    @Schema(description = "Done count")
    private long doneCount;

    @Schema(description = "IN_PROGRESS only (excludes IN_REVIEW)")
    private long inProgressCount;

    @Schema(description = "IN_REVIEW count")
    private long inReviewCount;

    @Schema(description = "TODO count")
    private long todoCount;

    @Schema(description = "CANCELLED count")
    private long cancelledCount;

    @Schema(description = "Overdue count (open items past due as of overdueAsOf date)")
    private long overdueCount;

    @Schema(description = "Completion percentage (0-100)")
    private double completionPercentage;

    @Schema(description = "Root tasks and subtasks with no assignee (within scope)")
    private long unassignedCount;

    @Schema(description = "Created in the activity range (inclusive dates), when provided")
    @Builder.Default
    private long createdInRangeCount = 0;

    @Schema(description = "Marked DONE with updatedAt in the activity range (approximate), when provided")
    @Builder.Default
    private long completedInRangeCount = 0;

    @Schema(description = "Counts per priority (all enum values listed, zero when none)")
    @Builder.Default
    private List<TaskPriorityCountDto> priorityBreakdown = new ArrayList<>();

    /** Distinct project IDs where this freelancer has at least one assigned root task or subtask (freelancer extended stats only). */
    @Schema(description = "Projects in which the freelancer has assigned tasks/subtasks; empty for non-freelancer scopes")
    @Builder.Default
    private List<Long> projectIdsWithAssignedWork = new ArrayList<>();
}
