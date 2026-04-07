package org.example.vendor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vendor.dto.request.VendorApprovalRequest;
import org.example.vendor.dto.request.VendorSignatureRequest;
import org.example.vendor.dto.response.EligibilityDetailResponse;
import org.example.vendor.dto.response.MatchProfileResponse;
import org.example.vendor.dto.response.MatchRecommendationResponse;
import org.example.vendor.dto.response.VendorAiDashboardResponse;
import org.example.vendor.dto.response.VendorApprovalResponse;
import org.example.vendor.dto.response.VendorAuditEntryResponse;
import org.example.vendor.dto.response.VendorDecisionInsightResponse;
import org.example.vendor.dto.response.VendorRenewalPreviewResponse;
import org.example.vendor.dto.response.VendorTrustScoreResponse;
import org.example.vendor.entity.VendorApprovalStatus;
import org.example.vendor.service.VendorAiDashboardService;
import org.example.vendor.service.VendorApprovalService;
import org.example.vendor.service.VendorDecisionInsightService;
import org.example.vendor.service.VendorMatchingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorApprovalController {

    private final VendorApprovalService service;
    private final VendorDecisionInsightService decisionInsightService;
    private final VendorAiDashboardService aiDashboardService;
    private final VendorMatchingService matchingService;

    // ── CRUD ───────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<VendorApprovalResponse> create(@Valid @RequestBody VendorApprovalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorApprovalResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<VendorApprovalResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<VendorApprovalResponse>> getByFreelancer(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(service.getByFreelancer(freelancerId));
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<List<VendorApprovalResponse>> getByOrganization(@PathVariable Long orgId) {
        return ResponseEntity.ok(service.getByOrganization(orgId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<VendorApprovalResponse>> getByStatus(@PathVariable VendorApprovalStatus status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Workflow d'agrément (transitions contrôlées + notifications proactives) ──

    @PatchMapping("/{id}/approve")
    public ResponseEntity<VendorApprovalResponse> approve(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(service.approve(id, adminId, notes));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<VendorApprovalResponse> reject(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(service.reject(id, reason));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<VendorApprovalResponse> suspend(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(service.suspend(id, reason));
    }

    /** Révocation d'urgence : suspend + bloque immédiatement les candidatures actives liées. */
    @PatchMapping("/{id}/emergency-revoke")
    public ResponseEntity<VendorApprovalResponse> emergencyRevoke(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(service.emergencyRevoke(id, reason));
    }

    @PatchMapping("/{id}/resubmit")
    public ResponseEntity<VendorApprovalResponse> resubmit(@PathVariable Long id) {
        return ResponseEntity.ok(service.resubmit(id));
    }

    @PostMapping("/{id}/sign/client")
    public ResponseEntity<VendorApprovalResponse> signAsClient(
            @PathVariable Long id,
            @Valid @RequestBody VendorSignatureRequest request) {
        return ResponseEntity.ok(service.signAsClient(id, request));
    }

    @PostMapping("/{id}/sign/freelancer")
    public ResponseEntity<VendorApprovalResponse> signAsFreelancer(
            @PathVariable Long id,
            @Valid @RequestBody VendorSignatureRequest request) {
        return ResponseEntity.ok(service.signAsFreelancer(id, request));
    }

    // ── Révision périodique + renouvellement semi-automatique ──

    /** Preview pré-rempli basé sur l'agrément précédent. */
    @GetMapping("/{id}/renewal-preview")
    public ResponseEntity<VendorRenewalPreviewResponse> renewalPreview(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRenewalPreview(id));
    }

    @PatchMapping("/{id}/renew")
    public ResponseEntity<VendorApprovalResponse> renew(
            @PathVariable Long id,
            @RequestParam Long adminId) {
        return ResponseEntity.ok(service.renew(id, adminId));
    }

    @PostMapping("/expire-outdated")
    public ResponseEntity<Map<String, Integer>> expireOutdated() {
        int count = service.expireOutdated();
        return ResponseEntity.ok(Map.of("expiredCount", count));
    }

    @GetMapping("/reviews-due")
    public ResponseEntity<List<VendorApprovalResponse>> getReviewsDue() {
        return ResponseEntity.ok(service.getReviewsDue());
    }

    @GetMapping("/reviews-overdue")
    public ResponseEntity<List<VendorApprovalResponse>> getReviewsOverdue() {
        return ResponseEntity.ok(service.getReviewsOverdue());
    }

    // ── Rappels expiration ────────────────────────────────────

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<VendorApprovalResponse>> expiringSoon(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(service.getExpiringSoon(days));
    }

    @PostMapping("/send-expiry-reminders")
    public ResponseEntity<Map<String, Integer>> sendExpiryReminders() {
        return ResponseEntity.ok(Map.of("remindersSent", service.runExpiryReminders()));
    }

    // ── Contrôle d'éligibilité ────────────────────────────────

    @GetMapping("/eligibility")
    public ResponseEntity<Map<String, Boolean>> checkEligibility(
            @RequestParam Long organizationId,
            @RequestParam Long freelancerId) {
        boolean eligible = service.isFreelancerEligible(organizationId, freelancerId);
        return ResponseEntity.ok(Map.of("eligible", eligible));
    }

    @GetMapping("/eligibility/detail")
    public ResponseEntity<EligibilityDetailResponse> eligibilityDetail(
            @RequestParam Long organizationId,
            @RequestParam Long freelancerId,
            @RequestParam(required = false) String domain) {
        return ResponseEntity.ok(service.getEligibilityDetail(organizationId, freelancerId, domain));
    }

    // ── Score de confiance freelancer ─────────────────────────

    @GetMapping("/trust-score/{freelancerId}")
    public ResponseEntity<VendorTrustScoreResponse> trustScore(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(service.computeTrustScore(freelancerId));
    }

    // ── Predictive AI Dashboard ───────────────────────────────

    @GetMapping("/ai-dashboard")
    public ResponseEntity<VendorAiDashboardResponse> aiDashboard() {
        return ResponseEntity.ok(aiDashboardService.buildDashboard());
    }

    // ── Aide à la décision admin ──────────────────────────────

    @GetMapping("/{id}/decision-insight")
    public ResponseEntity<VendorDecisionInsightResponse> decisionInsight(@PathVariable Long id) {
        return ResponseEntity.ok(decisionInsightService.buildInsight(id));
    }

    @GetMapping("/{id}/decision-insight/pdf")
    public ResponseEntity<byte[]> decisionInsightPdf(@PathVariable Long id) {
        byte[] pdf = decisionInsightService.buildPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"agrement-" + id + "-rapport.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Stats / audit ─────────────────────────────────────────

    @GetMapping("/{id}/audit-history")
    public ResponseEntity<List<VendorAuditEntryResponse>> auditHistory(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAuditHistory(id));
    }

    @GetMapping("/stats/organization/{orgId}")
    public ResponseEntity<Map<String, Long>> statsOrganization(@PathVariable Long orgId) {
        return ResponseEntity.ok(Map.of("approvedCount", service.countApprovedForOrganization(orgId)));
    }

    @GetMapping("/stats/freelancer/{fId}")
    public ResponseEntity<Map<String, Long>> statsFreelancer(@PathVariable Long fId) {
        return ResponseEntity.ok(Map.of("approvedCount", service.countApprovedForFreelancer(fId)));
    }

    // ── Talent Matching & Recommandations ─────────────────────

    /** Profil de matching agrégé d'un freelancer (compute ou cache). */
    @GetMapping("/matching/profile/{freelancerId}")
    public ResponseEntity<MatchProfileResponse> matchProfile(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(matchingService.getProfile(freelancerId));
    }

    /** Recalculer le profil de matching d'un freelancer. */
    @PostMapping("/matching/profile/{freelancerId}/refresh")
    public ResponseEntity<MatchProfileResponse> refreshProfile(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(matchingService.computeProfile(freelancerId));
    }

    /** Batch : recalculer tous les profils (cron ou manuel). */
    @PostMapping("/matching/refresh-all")
    public ResponseEntity<Map<String, Integer>> refreshAllProfiles() {
        return ResponseEntity.ok(Map.of("refreshed", matchingService.refreshAllProfiles()));
    }

    /** Top N freelancers par score global. */
    @GetMapping("/matching/top-freelancers")
    public ResponseEntity<List<MatchProfileResponse>> topFreelancers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(matchingService.getTopFreelancers(limit));
    }

    /** Freelancers avec agrément vendor actif (boosted en recherche). */
    @GetMapping("/matching/vendor-boosted")
    public ResponseEntity<List<MatchProfileResponse>> vendorBoosted() {
        return ResponseEntity.ok(matchingService.getVendorBoostedFreelancers());
    }

    /** Générer les top recommandations de freelancers pour un projet/offre. */
    @PostMapping("/matching/recommend")
    public ResponseEntity<List<MatchRecommendationResponse>> generateRecommendations(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam List<String> skills,
            @RequestParam(defaultValue = "10") int topN) {
        return ResponseEntity.ok(matchingService.generateRecommendations(targetType, targetId, skills, topN));
    }

    /** Recommandations existantes pour un projet/offre. */
    @GetMapping("/matching/recommendations")
    public ResponseEntity<List<MatchRecommendationResponse>> getRecommendations(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return ResponseEntity.ok(matchingService.getRecommendationsForTarget(targetType, targetId));
    }

    /** Projets/offres recommandés pour un freelancer. */
    @GetMapping("/matching/recommendations/freelancer/{freelancerId}")
    public ResponseEntity<List<MatchRecommendationResponse>> getFreelancerRecommendations(
            @PathVariable Long freelancerId) {
        return ResponseEntity.ok(matchingService.getRecommendationsForFreelancer(freelancerId));
    }

    /** Mettre à jour le statut d'une recommandation (VIEWED, CONTACTED, HIRED, DISMISSED). */
    @PatchMapping("/matching/recommendations/{id}/status")
    public ResponseEntity<MatchRecommendationResponse> updateRecommendationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(matchingService.updateRecommendationStatus(id, status));
    }
}
