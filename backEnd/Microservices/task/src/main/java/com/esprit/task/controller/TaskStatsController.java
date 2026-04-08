package com.esprit.task.controller;

import com.esprit.task.dto.TaskStatsDto;
import com.esprit.task.dto.TaskStatsExtendedDto;
import com.esprit.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Task Stats", description = "Task statistics by project, freelancer, and dashboard")
public class TaskStatsController {

    private final TaskService taskService;

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Stats by project", description = "Returns task counts and completion percentage for a project.")
    @ApiResponse(responseCode = "200", description = "Success")
    // Returns by project.
    public ResponseEntity<TaskStatsDto> getByProject(@Parameter(description = "Project ID", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getStatsByProject(projectId));
    }

    @GetMapping("/freelancer/{freelancerId}")
    @Operation(summary = "Stats by freelancer", description = "Returns task stats for the given assignee, with optional date range.")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<TaskStatsDto> getByFreelancer(
            @Parameter(description = "Freelancer ID", required = true) @PathVariable Long freelancerId,
            @Parameter(description = "From date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "To date (yyyy-MM-dd)") @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(taskService.getStatsByFreelancer(freelancerId, Optional.ofNullable(from), Optional.ofNullable(to)));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats", description = "Returns global task counts.")
    @ApiResponse(responseCode = "200", description = "Success")
    // Returns dashboard.
    public ResponseEntity<TaskStatsDto> getDashboard() {
        return ResponseEntity.ok(taskService.getDashboardStats());
    }

    @GetMapping("/extended/project/{projectId}")
    @Operation(summary = "Extended stats by project", description = "Root tasks and subtasks: status split, priority breakdown, unassigned. "
            + "Overdue uses today. createdInRangeCount / completedInRangeCount are zero (use weekly PDF or freelancer range for activity windows).")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<TaskStatsExtendedDto> getExtendedByProject(
            @Parameter(description = "Project ID", required = true) @PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getExtendedStatsByProject(projectId));
    }

    @GetMapping("/extended/freelancer/{freelancerId}")
    @Operation(summary = "Extended stats by freelancer", description = "Same optional from/to as freelancer stats (due date filter on assigned work). "
            + "When both from and to are set, also fills createdInRangeCount and completedInRangeCount for that inclusive period (completed ≈ DONE with updatedAt in range).")
    @ApiResponse(responseCode = "200", description = "Success")
    public ResponseEntity<TaskStatsExtendedDto> getExtendedByFreelancer(
            @Parameter(description = "Freelancer ID", required = true) @PathVariable Long freelancerId,
            @Parameter(description = "Due date from (yyyy-MM-dd)") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "Due date to (yyyy-MM-dd)") @RequestParam(required = false) LocalDate to) {
        Optional<LocalDate> dueFrom = Optional.ofNullable(from);
        Optional<LocalDate> dueTo = Optional.ofNullable(to);
        Optional<LocalDate> activityFrom = dueFrom.isPresent() && dueTo.isPresent() ? dueFrom : Optional.empty();
        Optional<LocalDate> activityTo = dueFrom.isPresent() && dueTo.isPresent() ? dueTo : Optional.empty();
        return ResponseEntity.ok(taskService.getExtendedStatsByFreelancer(freelancerId, dueFrom, dueTo, activityFrom, activityTo));
    }

    @GetMapping("/extended/dashboard")
    @Operation(summary = "Extended dashboard stats", description = "Global extended metrics; activity range counts are zero.")
    @ApiResponse(responseCode = "200", description = "Success")
    // Returns extended dashboard.
    public ResponseEntity<TaskStatsExtendedDto> getExtendedDashboard() {
        return ResponseEntity.ok(taskService.getExtendedStatsDashboard());
    }
}
