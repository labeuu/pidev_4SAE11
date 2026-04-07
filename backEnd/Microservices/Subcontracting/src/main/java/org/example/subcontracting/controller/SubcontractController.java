package org.example.subcontracting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.dto.request.SubcontractRequest;
import org.example.subcontracting.dto.response.*;
import org.example.subcontracting.service.SubcontractAuditService;
import org.example.subcontracting.service.SubcontractDashboardService;
import org.example.subcontracting.service.SubcontractNotificationService;
import org.example.subcontracting.service.SubcontractService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcontracts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubcontractController {

    private final SubcontractService service;
    private final SubcontractDashboardService dashboardService;
    private final SubcontractNotificationService notificationService;
    private final SubcontractAuditService auditService;

    // ── CRUD ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<SubcontractResponse> create(
            @RequestParam Long mainFreelancerId,
            @Valid @RequestBody SubcontractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(mainFreelancerId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubcontractResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<SubcontractResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<SubcontractResponse>> getByMainFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(service.getByMainFreelancer(freelancerId));
    }

    @GetMapping("/subcontractor/{subcontractorId}")
    public ResponseEntity<List<SubcontractResponse>> getBySubcontractor(@PathVariable Long subcontractorId) {
        return ResponseEntity.ok(service.getBySubcontractor(subcontractorId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SubcontractResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getByProject(projectId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SubcontractResponse>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubcontractResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubcontractRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Workflow ──────────────────────────────────────────────

    @PatchMapping("/{id}/propose")
    public ResponseEntity<SubcontractResponse> propose(@PathVariable Long id) {
        return ResponseEntity.ok(service.propose(id));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<SubcontractResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(service.accept(id));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<SubcontractResponse> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.reject(id, reason));
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<SubcontractResponse> startWork(@PathVariable Long id) {
        return ResponseEntity.ok(service.startWork(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<SubcontractResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(service.complete(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<SubcontractResponse> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.cancel(id, reason));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<SubcontractResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(service.close(id));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<SubcontractResponse> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(service.reopen(id));
    }

    // ── MÉTIER 3 — Score de performance ──────────────

    @GetMapping("/score/{subcontractorId}")
    public ResponseEntity<SubcontractorScoreResponse> getScore(@PathVariable Long subcontractorId) {
        return ResponseEntity.ok(dashboardService.computeScore(subcontractorId));
    }

    // ── MÉTIER 4 — Dashboard & statistiques ──────────

    @GetMapping("/dashboard")
    public ResponseEntity<SubcontractDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.buildDashboard());
    }

    // ── MÉTIER 2 — Triggers manuels (aussi en cron) ──

    @PostMapping("/check-overdue")
    public ResponseEntity<java.util.Map<String, Integer>> checkOverdue() {
        return ResponseEntity.ok(java.util.Map.of("alertsSent", notificationService.checkOverdueDeliverables()));
    }

    @PostMapping("/send-reminders")
    public ResponseEntity<java.util.Map<String, Integer>> sendReminders() {
        return ResponseEntity.ok(java.util.Map.of("remindersSent", notificationService.sendDeadlineReminders()));
    }

    // ── MÉTIER 5 — Historique & Timeline ─────────────

    @GetMapping("/{id}/history")
    public ResponseEntity<List<AuditTimelineEntry>> getSubcontractHistory(@PathVariable Long id) {
        return ResponseEntity.ok(auditService.getBySubcontract(id));
    }

    @GetMapping("/history/freelancer/{userId}")
    public ResponseEntity<FreelancerHistoryResponse> getFreelancerHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(auditService.getFreelancerHistory(userId));
    }
}
