package com.esprit.task.controller;

import com.esprit.task.service.TaskWeeklyReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Task reports", description = "Exported reports (PDF)")
public class TaskReportController {

    private final TaskWeeklyReportService taskWeeklyReportService;

    @GetMapping(value = "/weekly.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Task work report (PDF)", description = "Download a PDF. "
            + "Default: ISO week (weekStart optional; normalized to Monday of that week). "
            + "If lastDays is set, weekStart is ignored and the report uses a rolling inclusive window ending on periodEnd (default today).")
    @ApiResponse(responseCode = "200", description = "PDF bytes")
    public ResponseEntity<byte[]> getWeeklyPdf(
            @Parameter(description = "Monday of the week to report (yyyy-MM-dd); normalized to Monday if another weekday is sent")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Parameter(description = "Restrict to project")
            @RequestParam(required = false) Long projectId,
            @Parameter(description = "Restrict to assignee / freelancer")
            @RequestParam(required = false) Long freelancerId,
            @Parameter(description = "Rolling window length in days (1–366). When set, uses periodEnd and ignores weekStart.")
            @RequestParam(required = false) Integer lastDays,
            @Parameter(description = "Last day of the rolling window (yyyy-MM-dd); defaults to today when lastDays is set")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        Optional<Long> project = Optional.ofNullable(projectId);
        Optional<Long> freelancer = Optional.ofNullable(freelancerId);

        if (lastDays != null && lastDays > 0) {
            LocalDate end = periodEnd != null ? periodEnd : LocalDate.now();
            byte[] pdf = taskWeeklyReportService.buildRollingPeriodPdf(project, freelancer, end, lastDays);
            LocalDate start = end.minusDays(Math.min(Math.max(lastDays, 1), 366) - 1L);
            String filename = "task-report-" + start + "-to-" + end + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        }

        LocalDate monday = TaskWeeklyReportService.normalizeWeekStartMonday(weekStart);
        byte[] pdf = taskWeeklyReportService.buildWeeklyPdf(project, freelancer, weekStart);
        String filename = "task-report-week-" + monday + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
