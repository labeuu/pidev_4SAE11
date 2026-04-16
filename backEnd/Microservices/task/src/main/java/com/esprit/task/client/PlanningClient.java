package com.esprit.task.client;

import com.esprit.task.dto.planning.PlanningProgressReportDto;
import com.esprit.task.dto.planning.PlanningProgressUpdateCreateDto;
import com.esprit.task.dto.planning.PlanningProgressUpdateDto;
import com.esprit.task.dto.planning.PlanningProgressUpdateRefDto;
import com.esprit.task.dto.planning.PlanningProjectProgressStatsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for the Planning microservice progress-update APIs (read + create).
 * Used by {@link com.esprit.task.service.TaskAiService#clientStatusBrief} and
 * {@link com.esprit.task.service.TaskStatusProgressBridge}.
 *
 * <p>Resolved through Eureka with service ID {@code planning}.
 */
@FeignClient(
        name = "planning",
        contextId = "planningClient")
public interface PlanningClient {

    /**
     * All progress updates for a project (may be large; callers should cap client-side).
     */
    @GetMapping("/api/progress-updates/project/{projectId}")
    List<PlanningProgressUpdateDto> listProgressUpdatesByProject(@PathVariable("projectId") Long projectId);

    /** Aggregated stats for grounding narrative when the update list is sparse. */
    @GetMapping("/api/progress-updates/stats/project/{projectId}")
    PlanningProjectProgressStatsDto getProjectProgressStats(@PathVariable("projectId") Long projectId);

    /**
     * Optional inclusive date range report; Planning defaults to last 30 days if dates omitted.
     */
    @GetMapping("/api/progress-updates/stats/report")
    PlanningProgressReportDto getProgressReport(
            @RequestParam("projectId") Long projectId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    /** Creates a progress update (201 + body). Callers should handle {@link feign.FeignException} and other failures. */
    @PostMapping("/api/progress-updates")
    PlanningProgressUpdateRefDto createProgressUpdate(@RequestBody PlanningProgressUpdateCreateDto body);
}
