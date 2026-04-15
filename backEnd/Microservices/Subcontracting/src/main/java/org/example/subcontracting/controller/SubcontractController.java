package org.example.subcontracting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.dto.request.SubcontractAiMatchRequest;
import org.example.subcontracting.dto.request.CounterOfferRequest;
import org.example.subcontracting.dto.request.AiMediateRequest;
import org.example.subcontracting.dto.request.SubcontractRiskCockpitRequest;
import org.example.subcontracting.dto.request.SubcontractRiskConfirmRequest;
import org.example.subcontracting.dto.request.SubcontractRequest;
import org.example.subcontracting.dto.response.*;
import org.example.subcontracting.service.SubcontractAiMatchService;
import org.example.subcontracting.service.SubcontractFinancialAnalysisService;
import org.example.subcontracting.service.SubcontractAuditService;
import org.example.subcontracting.service.SubcontractDashboardService;
import org.example.subcontracting.service.SubcontractNotificationService;
import org.example.subcontracting.service.SubcontractCoachingService;
import org.example.subcontracting.service.SubcontractAiDecisionAssistantService;
import org.example.subcontracting.service.SubcontractPredictiveDashboardService;
import org.example.subcontracting.service.SubcontractRiskCockpitService;
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
    private final SubcontractAiMatchService aiMatchService;
    private final SubcontractFinancialAnalysisService financialAnalysisService;
    private final SubcontractRiskCockpitService riskCockpitService;
    private final SubcontractPredictiveDashboardService predictiveDashboardService;
    private final SubcontractCoachingService coachingService;
    private final SubcontractAiDecisionAssistantService aiDecisionAssistantService;

    // ── IA — matching sous-traitant ───────────────────────────

    @PostMapping("/ai/match-subcontractor")
    public ResponseEntity<SubcontractMatchResponse> matchSubcontractor(
            @RequestParam Long mainFreelancerId,
            @Valid @RequestBody SubcontractAiMatchRequest body) {
        return ResponseEntity.ok(aiMatchService.matchSubcontractors(mainFreelancerId, body.getRequiredSkills()));
    }

    @GetMapping("/{id}/ai/financial-analysis")
    public ResponseEntity<SubcontractFinancialAnalysisResponse> financialAnalysis(
            @PathVariable Long id,
            @RequestParam Long mainFreelancerId) {
        return ResponseEntity.ok(financialAnalysisService.analyze(id, mainFreelancerId));
    }

    @GetMapping("/ai/predictive-dashboard")
    public ResponseEntity<PredictiveDashboardResponse> predictiveDashboard(
            @RequestParam Long mainFreelancerId) {
        return ResponseEntity.ok(predictiveDashboardService.buildDashboard(mainFreelancerId));
    }

    @GetMapping("/ai/my-coaching-profile")
    public ResponseEntity<MyCoachingProfileResponse> myCoachingProfile(
            @RequestParam Long mainFreelancerId) {
        return ResponseEntity.ok(coachingService.getProfile(mainFreelancerId));
    }

    @GetMapping("/ai/risk-score")
    public ResponseEntity<RiskScoreResponse> riskScore(
            @RequestParam Long subcontractId,
            @RequestParam(required = false) Long mainFreelancerId) {
        return ResponseEntity.ok(aiDecisionAssistantService.riskScore(mainFreelancerId, subcontractId));
    }

    @GetMapping("/ai/traps-detected")
    public ResponseEntity<TrapDetectionResponse> trapsDetected(
            @RequestParam Long subcontractId,
            @RequestParam(required = false) Long mainFreelancerId) {
        return ResponseEntity.ok(aiDecisionAssistantService.trapsDetected(mainFreelancerId, subcontractId));
    }

    @GetMapping("/ai/match-freelancers")
    public ResponseEntity<FreelancerMatchDecisionResponse> matchFreelancers(
            @RequestParam Long subcontractId,
            @RequestParam(required = false) Long mainFreelancerId,
            @RequestParam(defaultValue = "5") Integer top) {
        return ResponseEntity.ok(aiDecisionAssistantService.matchFreelancers(mainFreelancerId, subcontractId, top == null ? 5 : top));
    }

    @GetMapping("/ai/predict-failure")
    public ResponseEntity<FailurePredictionResponse> predictFailure(
            @RequestParam Long subcontractId,
            @RequestParam(required = false) Long mainFreelancerId) {
        return ResponseEntity.ok(aiDecisionAssistantService.predictFailure(mainFreelancerId, subcontractId));
    }

    @PostMapping("/ai/risk-cockpit")
    public ResponseEntity<SubcontractRiskCockpitResponse> riskCockpit(
            @Valid @RequestBody SubcontractRiskCockpitRequest body) {
        return ResponseEntity.ok(riskCockpitService.analyze(body));
    }

    @GetMapping(path = "/ai/risk-cockpit/stream", produces = "text/event-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter riskCockpitStream(
            @RequestParam Long mainFreelancerId,
            @RequestParam(required = false) Long subcontractorId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long offerId,
            @RequestParam(required = false) java.math.BigDecimal budget,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String deadline,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) List<String> requiredSkills) {
        SubcontractRiskCockpitRequest req = new SubcontractRiskCockpitRequest();
        req.setMainFreelancerId(mainFreelancerId);
        req.setSubcontractorId(subcontractorId);
        req.setProjectId(projectId);
        req.setOfferId(offerId);
        req.setBudget(budget);
        req.setStartDate(startDate);
        req.setDeadline(deadline);
        req.setScope(scope);
        req.setRequiredSkills(requiredSkills);
        return riskCockpitService.stream(req);
    }

    @PostMapping("/ai/risk-cockpit/simulate")
    public ResponseEntity<java.util.Map<String, Object>> auditSimulation(
            @RequestParam Long mainFreelancerId,
            @Valid @RequestBody SubcontractRiskCockpitResponse body) {
        auditService.record(
                0L,
                mainFreelancerId,
                "AI_RISK_SIMULATED",
                null,
                null,
                "Simulation risque: score=" + body.getTotalRiskScore() + ", level=" + body.getLevel(),
                "SUBCONTRACT_DRAFT",
                null
        );
        return ResponseEntity.ok(java.util.Map.of("recorded", true));
    }

    @PostMapping("/{id}/ai/risk-confirm")
    public ResponseEntity<java.util.Map<String, Object>> confirmRisk(
            @PathVariable Long id,
            @RequestParam Long mainFreelancerId,
            @Valid @RequestBody SubcontractRiskConfirmRequest body) {
        auditService.record(
                id,
                mainFreelancerId,
                "AI_RISK_CONFIRMED",
                null,
                null,
                "Validation finale risque: score=" + body.getTotalRiskScore()
                        + ", alternative=" + body.getSelectedAlternativeLabel()
                        + ", note=" + body.getSummary(),
                "SUBCONTRACT",
                id
        );
        return ResponseEntity.ok(java.util.Map.of("recorded", true));
    }

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

    @PostMapping("/{id}/counter-offer")
    public ResponseEntity<NegotiationRoundResponse> counterOffer(
            @PathVariable Long id,
            @RequestParam Long subcontractorId,
            @Valid @RequestBody CounterOfferRequest request) {
        return ResponseEntity.ok(service.counterOffer(id, subcontractorId, request));
    }

    @PostMapping("/{id}/ai/mediate")
    public ResponseEntity<NegotiationRoundResponse> aiMediate(
            @PathVariable Long id,
            @RequestParam Long mainFreelancerId,
            @RequestBody(required = false) AiMediateRequest request) {
        return ResponseEntity.ok(service.aiMediate(id, mainFreelancerId, request));
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
