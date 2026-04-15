package com.esprit.planning.controller;

import com.esprit.planning.dto.ContractProgressStatsDto;
import com.esprit.planning.dto.DashboardStatsDto;
import com.esprit.planning.dto.FreelancerProgressStatsDto;
import com.esprit.planning.dto.ProgressReportDto;
import com.esprit.planning.dto.ProjectProgressStatsDto;
import com.esprit.planning.service.FreelancerProjectAccessService;
import com.esprit.planning.service.ProgressUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

/**
 * REST API for progress update statistics: stats by freelancer, project, contract, dashboard aggregate, and time-bounded project report.
 * All endpoints are under /api/progress-updates/stats.
 */
@RestController
@RequestMapping("/api/progress-updates/stats")
@CrossOrigin(origins = "*")
@Tag(name = "Progress Update Statistics", description = "Statistics and dashboard stats for progress updates")
public class ProgressUpdateStatsController {

    private final ProgressUpdateService progressUpdateService;
    private final ObjectProvider<FreelancerProjectAccessService> freelancerProjectAccessServiceProvider;

    public ProgressUpdateStatsController(ProgressUpdateService progressUpdateService,
                                         ObjectProvider<FreelancerProjectAccessService> freelancerProjectAccessServiceProvider) {
        this.progressUpdateService = progressUpdateService;
        this.freelancerProjectAccessServiceProvider = freelancerProjectAccessServiceProvider;
    }

    /** Returns progress statistics for the given freelancer (update count, comments, average %, last update, etc.). */
    @GetMapping("/freelancer/{freelancerId}")
    @Operation(summary = "Stats by freelancer", description = "Returns progress statistics for the given freelancer.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = FreelancerProgressStatsDto.class)))
    public ResponseEntity<FreelancerProgressStatsDto> getStatsByFreelancer(
            @Parameter(description = "Freelancer ID", example = "10", required = true) @PathVariable Long freelancerId) {
        return ResponseEntity.ok(progressUpdateService.getProgressStatisticsByFreelancer(freelancerId));
    }

    /** Returns progress statistics for the given project (update count, comments, current %, first/last update). */
    @GetMapping("/project/{projectId}")
    @Operation(summary = "Stats by project", description = "Returns progress statistics for the given project.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ProjectProgressStatsDto.class)))
    public ResponseEntity<ProjectProgressStatsDto> getStatsByProject(
            @Parameter(description = "Project ID", example = "1", required = true) @PathVariable Long projectId,
            @RequestHeader(value = "X-User-Id", required = false) Long viewerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String viewerRole) {
        return ResponseEntity.ok(progressUpdateService.getProgressStatisticsByProject(projectId, resolveFreelancerScope(viewerUserId, viewerRole)));
    }

    /** Returns progress statistics for the given contract. */
    @GetMapping("/contract/{contractId}")
    @Operation(summary = "Stats by contract", description = "Returns progress statistics for the given contract.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ContractProgressStatsDto.class)))
    public ResponseEntity<ContractProgressStatsDto> getStatsByContract(
            @Parameter(description = "Contract ID", example = "1", required = true) @PathVariable Long contractId,
            @RequestHeader(value = "X-User-Id", required = false) Long viewerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String viewerRole) {
        return ResponseEntity.ok(progressUpdateService.getProgressStatisticsByContract(contractId, resolveFreelancerScope(viewerUserId, viewerRole)));
    }

    /** Returns global dashboard statistics over all progress updates (total updates, comments, average %, distinct projects/freelancers). */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats", description = "Returns global dashboard statistics over all progress updates.")
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = DashboardStatsDto.class)))
    // Returns dashboard stats.
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(progressUpdateService.getDashboardStatistics());
    }

    /** Returns a time-bounded report for a single project (updates, comments, averages between from/to). Defaults to last 30 days if from/to omitted. */
    @GetMapping("/report")
    @Operation(
            summary = "Time-bounded project report",
            description = "Returns a time-bounded report (updates, comments, averages) for a single project between from/to dates. If from/to are omitted, defaults to the last 30 days."
    )
    @ApiResponse(responseCode = "200", description = "Success", content = @Content(schema = @Schema(implementation = ProgressReportDto.class)))
    public ResponseEntity<ProgressReportDto> getProjectReport(
            @Parameter(description = "Project ID", example = "1", required = true) @RequestParam Long projectId,
            @Parameter(description = "Start date (yyyy-MM-dd), optional") @RequestParam(required = false) java.time.LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd), optional") @RequestParam(required = false) java.time.LocalDate to,
            @RequestHeader(value = "X-User-Id", required = false) Long viewerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String viewerRole
    ) {
        return ResponseEntity.ok(progressUpdateService.getProgressReportForProject(
                projectId, from, to, resolveFreelancerScope(viewerUserId, viewerRole)));
    }

    private Optional<Set<Long>> resolveFreelancerScope(Long userId, String role) {
        if (!"FREELANCER".equalsIgnoreCase(role)) {
            return Optional.empty();
        }
        if (userId == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "X-User-Id is required for freelancer scope");
        }
        FreelancerProjectAccessService accessService = freelancerProjectAccessServiceProvider.getIfAvailable();
        if (accessService == null) {
            return Optional.empty();
        }
        return Optional.of(accessService.getAccessibleProjectIdsForFreelancer(userId));
    }
}
