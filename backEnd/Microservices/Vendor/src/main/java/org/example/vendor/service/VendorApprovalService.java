package org.example.vendor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vendor.client.OfferFeignClient;
import org.example.vendor.client.ReviewFeignClient;
import org.example.vendor.client.dto.ReviewStatsRemoteDto;
import org.example.vendor.dto.request.VendorApprovalRequest;
import org.example.vendor.dto.request.VendorSignatureRequest;
import org.example.vendor.dto.response.EligibilityDetailResponse;
import org.example.vendor.dto.response.VendorApprovalResponse;
import org.example.vendor.dto.response.VendorAuditEntryResponse;
import org.example.vendor.dto.response.VendorRenewalPreviewResponse;
import org.example.vendor.dto.response.VendorTrustScoreResponse;
import org.example.vendor.entity.VendorApproval;
import org.example.vendor.entity.VendorApprovalAudit;
import org.example.vendor.entity.VendorApprovalStatus;
import org.example.vendor.exception.BadRequestException;
import org.example.vendor.exception.ResourceNotFoundException;
import org.example.vendor.repository.VendorApprovalAuditRepository;
import org.example.vendor.repository.VendorApprovalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des fournisseurs (vendor management).
 *
 * Métiers avancés :
 *   1. Workflow d'agrément avec transitions contrôlées (machine à états) + notifications proactives
 *   2. Révision périodique automatique (scheduler + dates + listes dues / en retard)
 *   3. Contrôle d'éligibilité (booléen + détail avec motif et contrôle de domaine optionnel)
 *   4. Historique & audit (table vendor_approval_audits + stats agrégées)
 *   5. Rappels avant fin de validité (notification J-30, anti-doublon expiryReminderSentAt)
 *   6. Secteur métier (professionalSector) pour classement / reporting
 *   7. Renouvellement semi-automatique (preview pré-rempli)
 *   8. Score de confiance freelancer (trust score 0-100)
 *   9. Révocation d'urgence (suspend + blocage candidatures actives via Offer Feign)
 *  10. Multi-agréments par domaine (même org+freelancer, domaines différents)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorApprovalService {

    public static final int EXPIRY_REMINDER_DAYS_BEFORE = 30;

    private final VendorApprovalRepository repository;
    private final VendorApprovalAuditRepository auditRepository;
    private final VendorSignatureNotificationService signatureNotificationService;
    private final OfferFeignClient offerFeignClient;
    private final ReviewFeignClient reviewFeignClient;

    // ══════════════════════════════════════════════════════════
    //  CRUD (amélioré : multi-agréments par domaine)
    // ══════════════════════════════════════════════════════════

    public VendorApprovalResponse create(VendorApprovalRequest request) {
        String domain = request.getDomain() != null ? request.getDomain().trim() : "";

        if (!domain.isBlank()) {
            repository.findActiveDuplicate(
                    request.getOrganizationId(), request.getFreelancerId(), domain
            ).ifPresent(existing -> {
                throw new BadRequestException(
                        "Un agrément actif existe déjà pour ce freelancer/organisation sur le domaine « "
                                + domain + " » (statut : " + existing.getStatus() + ").");
            });
        } else {
            repository.findByOrganizationIdAndFreelancerId(
                    request.getOrganizationId(), request.getFreelancerId()
            ).ifPresent(existing -> {
                if (existing.getStatus() == VendorApprovalStatus.APPROVED
                        || existing.getStatus() == VendorApprovalStatus.PENDING) {
                    throw new BadRequestException(
                            "Un agrément existe déjà pour ce freelancer/organisation (statut : "
                                    + existing.getStatus() + "). Précisez un domaine différent pour un multi-agrément.");
                }
            });
        }

        VendorApproval va = new VendorApproval();
        va.setOrganizationId(request.getOrganizationId());
        va.setFreelancerId(request.getFreelancerId());
        va.setDomain(domain.isBlank() ? null : domain);
        if (request.getProfessionalSector() != null && !request.getProfessionalSector().isBlank()) {
            va.setProfessionalSector(request.getProfessionalSector().trim());
        }
        va.setApprovalNotes(buildAutomaticCreationNotes(request));
        va.setStatus(VendorApprovalStatus.PENDING);
        va = repository.save(va);

        appendAudit(va.getId(), null, VendorApprovalStatus.PENDING, "CREATED", null,
                va.getApprovalNotes());
        signatureNotificationService.notifySignatureRequired(va, null);
        signatureNotificationService.notifyStatusChanged(va, null, VendorApprovalStatus.PENDING, "Nouvel agrément créé.");
        log.info("[VENDOR] Créé : id={} org={} freelancer={} domain={}",
                va.getId(), va.getOrganizationId(), va.getFreelancerId(), va.getDomain());
        return toResponse(va);
    }

    @Transactional(readOnly = true)
    public VendorApprovalResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<VendorApprovalResponse> getByFreelancer(Long freelancerId) {
        return repository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VendorApprovalResponse> getByOrganization(Long organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VendorApprovalResponse> getAll() {
        return repository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public void delete(Long id) {
        VendorApproval va = findOrThrow(id);
        if (va.getStatus() == VendorApprovalStatus.APPROVED) {
            throw new BadRequestException("Impossible de supprimer un agrément actif. Suspendez-le d'abord.");
        }
        VendorApprovalStatus st = va.getStatus();
        appendAudit(va.getId(), st, st, "DELETED", null, "Suppression du dossier agrément.");
        repository.delete(va);
        log.info("[VENDOR] Supprimé : id={}", id);
    }

    // ══════════════════════════════════════════════════════════
    //  MÉTIER 1 — Workflow d'agrément (machine à états)
    //  + notifications proactives à chaque changement de statut
    // ══════════════════════════════════════════════════════════

    public VendorApprovalResponse approve(Long id, Long adminId, String notes) {
        VendorApproval va = findOrThrow(id);
        assertTransition(va, VendorApprovalStatus.APPROVED);
        VendorApprovalStatus prev = va.getStatus();
        if (prev == VendorApprovalStatus.PENDING) {
            if (va.getClientSignedAt() == null || va.getFreelancerSignedAt() == null) {
                throw new BadRequestException(
                        "Approbation impossible : le client et le freelancer doivent d'abord signer électroniquement l'agrément.");
            }
        }

        va.setStatus(VendorApprovalStatus.APPROVED);
        va.setApprovedBy(adminId);
        va.setApprovalNotes(notes);
        va.setValidFrom(LocalDate.now());
        va.setValidUntil(LocalDate.now().plusYears(1));
        va.setNextReviewDate(LocalDate.now().plusMonths(11));
        va.setExpiryReminderSentAt(null);
        va.setStatusChangedAt(LocalDateTime.now());
        va.setRejectionReason(null);
        va.setSuspensionReason(null);
        va = repository.save(va);

        appendAudit(va.getId(), prev, VendorApprovalStatus.APPROVED, "APPROVED", adminId, notes);
        signatureNotificationService.notifyStatusChanged(va, prev, VendorApprovalStatus.APPROVED, notes);
        log.info("[VENDOR] Approuvé id={} par admin={} jusqu'au {}", id, adminId, va.getValidUntil());
        return toResponse(va);
    }

    public VendorApprovalResponse reject(Long id, String reason) {
        VendorApproval va = findOrThrow(id);
        assertTransition(va, VendorApprovalStatus.REJECTED);
        VendorApprovalStatus prev = va.getStatus();

        va.setStatus(VendorApprovalStatus.REJECTED);
        va.setRejectionReason(reason);
        va.setStatusChangedAt(LocalDateTime.now());
        va = repository.save(va);

        appendAudit(va.getId(), prev, VendorApprovalStatus.REJECTED, "REJECTED", null, reason);
        signatureNotificationService.notifyStatusChanged(va, prev, VendorApprovalStatus.REJECTED, reason);
        log.info("[VENDOR] Rejeté id={} raison={}", id, reason);
        return toResponse(va);
    }

    public VendorApprovalResponse suspend(Long id, String reason) {
        VendorApproval va = findOrThrow(id);
        assertTransition(va, VendorApprovalStatus.SUSPENDED);
        VendorApprovalStatus prev = va.getStatus();

        va.setStatus(VendorApprovalStatus.SUSPENDED);
        va.setSuspensionReason(reason);
        va.setStatusChangedAt(LocalDateTime.now());
        va = repository.save(va);

        appendAudit(va.getId(), prev, VendorApprovalStatus.SUSPENDED, "SUSPENDED", null, reason);
        signatureNotificationService.notifyStatusChanged(va, prev, VendorApprovalStatus.SUSPENDED, reason);
        log.info("[VENDOR] Suspendu id={} raison={}", id, reason);
        return toResponse(va);
    }

    /**
     * Révocation d'urgence : suspend l'agrément ET bloque immédiatement
     * toutes les candidatures actives liées (via le microservice Offer).
     */
    public VendorApprovalResponse emergencyRevoke(Long id, String reason) {
        VendorApproval va = findOrThrow(id);
        if (va.getStatus() != VendorApprovalStatus.APPROVED) {
            throw new BadRequestException("La révocation d'urgence s'applique uniquement aux agréments APPROVED.");
        }
        VendorApprovalStatus prev = va.getStatus();

        va.setStatus(VendorApprovalStatus.SUSPENDED);
        va.setSuspensionReason("[URGENCE] " + reason);
        va.setStatusChangedAt(LocalDateTime.now());
        va = repository.save(va);

        int blocked = 0;
        try {
            blocked = offerFeignClient.blockApplicationsByVendor(
                    va.getOrganizationId(), va.getFreelancerId(),
                    "Agrément #" + va.getId() + " révoqué d'urgence : " + reason);
            log.info("[VENDOR] Révocation urgence — {} candidature(s) bloquée(s)", blocked);
        } catch (Exception e) {
            log.warn("[VENDOR] Révocation urgence — échec blocage candidatures : {}", e.getMessage());
        }

        appendAudit(va.getId(), prev, VendorApprovalStatus.SUSPENDED, "EMERGENCY_REVOKE", null,
                "Révocation d'urgence — " + blocked + " candidature(s) bloquée(s). Motif : " + reason);
        signatureNotificationService.notifyEmergencyRevoke(va, reason, blocked);
        return toResponse(va);
    }

    public VendorApprovalResponse resubmit(Long id) {
        VendorApproval va = findOrThrow(id);
        assertTransition(va, VendorApprovalStatus.PENDING);
        VendorApprovalStatus prev = va.getStatus();

        va.setStatus(VendorApprovalStatus.PENDING);
        va.setRejectionReason(null);
        va.setSuspensionReason(null);
        va.setClientSignedAt(null);
        va.setClientSignerName(null);
        va.setFreelancerSignedAt(null);
        va.setFreelancerSignerName(null);
        va.setStatusChangedAt(LocalDateTime.now());
        va = repository.save(va);

        appendAudit(va.getId(), prev, VendorApprovalStatus.PENDING, "RESUBMIT", null,
                "Re-soumission pour instruction.");
        signatureNotificationService.notifySignatureRequired(va, "Nouvelle signature requise après re-soumission.");
        signatureNotificationService.notifyStatusChanged(va, prev, VendorApprovalStatus.PENDING, "Re-soumission.");
        log.info("[VENDOR] Re-soumis id={}", id);
        return toResponse(va);
    }

    public VendorApprovalResponse signAsClient(Long id, VendorSignatureRequest req) {
        VendorApproval va = findOrThrow(id);
        if (va.getStatus() != VendorApprovalStatus.PENDING) {
            throw new BadRequestException("Signature client uniquement pour un agrément en attente.");
        }
        if (!va.getOrganizationId().equals(req.getSignerUserId())) {
            throw new BadRequestException("Seul le compte organisation de cet agrément peut signer ici.");
        }
        if (va.getClientSignedAt() != null) {
            throw new BadRequestException("Le client a déjà signé cet agrément.");
        }
        va.setClientSignedAt(LocalDateTime.now());
        va.setClientSignerName(req.getFullName().trim());
        va = repository.save(va);
        appendAudit(va.getId(), VendorApprovalStatus.PENDING, VendorApprovalStatus.PENDING, "SIGN_CLIENT",
                req.getSignerUserId(), "Signature client : " + va.getClientSignerName());
        if (va.getFreelancerSignedAt() != null) {
            signatureNotificationService.notifyBothPartiesFullySigned(va);
        } else {
            signatureNotificationService.notifyAfterClientSigned(va);
        }
        return toResponse(va);
    }

    public VendorApprovalResponse signAsFreelancer(Long id, VendorSignatureRequest req) {
        VendorApproval va = findOrThrow(id);
        if (va.getStatus() != VendorApprovalStatus.PENDING) {
            throw new BadRequestException("Signature freelancer uniquement pour un agrément en attente.");
        }
        if (!va.getFreelancerId().equals(req.getSignerUserId())) {
            throw new BadRequestException("Seul le freelancer concerné par cet agrément peut signer ici.");
        }
        if (va.getFreelancerSignedAt() != null) {
            throw new BadRequestException("Le freelancer a déjà signé cet agrément.");
        }
        va.setFreelancerSignedAt(LocalDateTime.now());
        va.setFreelancerSignerName(req.getFullName().trim());
        va = repository.save(va);
        appendAudit(va.getId(), VendorApprovalStatus.PENDING, VendorApprovalStatus.PENDING, "SIGN_FREELANCER",
                req.getSignerUserId(), "Signature freelancer : " + va.getFreelancerSignerName());
        if (va.getClientSignedAt() != null) {
            signatureNotificationService.notifyBothPartiesFullySigned(va);
        } else {
            signatureNotificationService.notifyAfterFreelancerSigned(va);
        }
        return toResponse(va);
    }

    // ══════════════════════════════════════════════════════════
    //  MÉTIER 2 — Révision périodique + renouvellement semi-auto
    // ══════════════════════════════════════════════════════════

    /**
     * Preview pré-rempli pour le renouvellement semi-automatique :
     * reprend domaine, secteur, notes de l'agrément précédent et propose
     * des dates (validFrom=today, validUntil=today+1an, nextReview=today+11m).
     */
    @Transactional(readOnly = true)
    public VendorRenewalPreviewResponse getRenewalPreview(Long id) {
        VendorApproval va = findOrThrow(id);
        boolean canRenew = va.getStatus() == VendorApprovalStatus.APPROVED;
        String reason = canRenew ? null : "Seuls les agréments APPROVED peuvent être renouvelés (statut actuel : " + va.getStatus() + ").";

        return VendorRenewalPreviewResponse.builder()
                .vendorApprovalId(va.getId())
                .organizationId(va.getOrganizationId())
                .freelancerId(va.getFreelancerId())
                .domain(va.getDomain())
                .professionalSector(va.getProfessionalSector())
                .currentReviewCount(va.getReviewCount())
                .previousValidFrom(va.getValidFrom())
                .previousValidUntil(va.getValidUntil())
                .suggestedValidFrom(LocalDate.now())
                .suggestedValidUntil(LocalDate.now().plusYears(1))
                .suggestedNextReviewDate(LocalDate.now().plusMonths(11))
                .previousApprovalNotes(va.getApprovalNotes())
                .canRenew(canRenew)
                .cannotRenewReason(reason)
                .build();
    }

    public VendorApprovalResponse renew(Long id, Long adminId) {
        VendorApproval va = findOrThrow(id);
        if (va.getStatus() != VendorApprovalStatus.APPROVED) {
            throw new BadRequestException("Seuls les agréments APPROVED peuvent être renouvelés.");
        }

        va.setValidFrom(LocalDate.now());
        va.setValidUntil(LocalDate.now().plusYears(1));
        va.setNextReviewDate(LocalDate.now().plusMonths(11));
        va.setReviewCount(va.getReviewCount() + 1);
        va.setApprovedBy(adminId);
        va.setExpiryReminderSentAt(null);
        va.setStatusChangedAt(LocalDateTime.now());
        va = repository.save(va);

        appendAudit(va.getId(), VendorApprovalStatus.APPROVED, VendorApprovalStatus.APPROVED, "RENEW", adminId,
                "Renouvellement périodique — reviewCount=" + va.getReviewCount() + ", validUntil=" + va.getValidUntil());
        signatureNotificationService.notifyStatusChanged(va, VendorApprovalStatus.APPROVED, VendorApprovalStatus.APPROVED,
                "Agrément renouvelé jusqu'au " + va.getValidUntil() + ".");
        log.info("[VENDOR] Renouvelé id={} reviewCount={} jusqu'au {}", id, va.getReviewCount(), va.getValidUntil());
        return toResponse(va);
    }

    /**
     * Scheduler : expire automatiquement les agréments dont validUntil est dépassée.
     * Exécuté chaque jour à 2h du matin. Envoie une notification pour chaque expiration.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public int expireOutdated() {
        List<VendorApproval> expired = repository.findExpiredApprovals(LocalDate.now());
        int count = 0;
        for (VendorApproval va : expired) {
            VendorApprovalStatus prev = va.getStatus();
            va.setStatus(VendorApprovalStatus.EXPIRED);
            va.setStatusChangedAt(LocalDateTime.now());
            repository.save(va);
            appendAudit(va.getId(), prev, VendorApprovalStatus.EXPIRED, "AUTO_EXPIRE", null,
                    "Expiration automatique (validUntil dépassée).");
            signatureNotificationService.notifyStatusChanged(va, prev, VendorApprovalStatus.EXPIRED,
                    "L'agrément a expiré automatiquement (fin de validité : " + va.getValidUntil() + ").");
            count++;
            log.info("[VENDOR] Auto-expiré id={}", va.getId());
        }
        if (count > 0) {
            log.info("[VENDOR] {} agrément(s) expiré(s) automatiquement", count);
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<VendorApprovalResponse> getReviewsDue() {
        LocalDate in30Days = LocalDate.now().plusDays(30);
        return repository.findReviewsDueBefore(in30Days)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VendorApprovalResponse> getReviewsOverdue() {
        return repository.findReviewsOverdue(LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════
    //  MÉTIER 5 — Rappels avant expiration de validité
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<VendorApprovalResponse> getExpiringSoon(int days) {
        int d = days < 1 ? EXPIRY_REMINDER_DAYS_BEFORE : days;
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(d);
        return repository.findApprovedExpiringWithin(today, until).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public int runExpiryReminders() {
        LocalDate targetDay = LocalDate.now().plusDays(EXPIRY_REMINDER_DAYS_BEFORE);
        List<VendorApproval> due = repository.findApprovedExpiringOn(targetDay);
        int sent = 0;
        for (VendorApproval va : due) {
            signatureNotificationService.notifyExpiryApproaching(va, EXPIRY_REMINDER_DAYS_BEFORE);
            va.setExpiryReminderSentAt(LocalDateTime.now());
            repository.save(va);
            appendAudit(va.getId(), VendorApprovalStatus.APPROVED, VendorApprovalStatus.APPROVED,
                    "EXPIRY_REMINDER", null,
                    "Rappel J-" + EXPIRY_REMINDER_DAYS_BEFORE + " avant fin de validité (" + va.getValidUntil() + ").");
            sent++;
            log.info("[VENDOR] Rappel expiration envoyé id={} validUntil={}", va.getId(), va.getValidUntil());
        }
        if (sent > 0) {
            log.info("[VENDOR] {} rappel(s) d'expiration envoyé(s)", sent);
        }
        return sent;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendExpiryRemindersScheduled() {
        runExpiryReminders();
    }

    // ══════════════════════════════════════════════════════════
    //  MÉTIER 3 — Contrôle d'éligibilité
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public boolean isFreelancerEligible(Long organizationId, Long freelancerId) {
        return getEligibilityDetail(organizationId, freelancerId, null).isEligible();
    }

    @Transactional(readOnly = true)
    public EligibilityDetailResponse getEligibilityDetail(Long organizationId, Long freelancerId, String requiredDomain) {
        return repository.findByOrganizationIdAndFreelancerId(organizationId, freelancerId)
                .map(va -> evaluateEligibility(va, requiredDomain))
                .orElse(EligibilityDetailResponse.no("NO_AGREEMENT",
                        "Aucun agrément pour cette organisation et ce freelancer."));
    }

    // ══════════════════════════════════════════════════════════
    //  MÉTIER 8 — Score de confiance freelancer (0-100)
    // ══════════════════════════════════════════════════════════

    /**
     * Calcule un indice de fiabilité du freelancer basé sur :
     * - Taux de renouvellement (25 pts max)
     * - Historique : rejets/suspensions pénalisent (20 pts max)
     * - Note moyenne reviews client→freelancer (30 pts max)
     * - Nombre d'agréments actifs (15 pts max)
     * - Ancienneté (10 pts max)
     */
    @Transactional(readOnly = true)
    public VendorTrustScoreResponse computeTrustScore(Long freelancerId) {
        List<VendorApproval> all = repository.findByFreelancerId(freelancerId);
        if (all.isEmpty()) {
            return VendorTrustScoreResponse.builder()
                    .freelancerId(freelancerId)
                    .score(0)
                    .label("Insuffisant")
                    .breakdown(List.of("Aucun agrément trouvé pour ce freelancer."))
                    .build();
        }

        long total = all.size();
        long active = all.stream().filter(VendorApproval::isActiveAndValid).count();
        long totalRenewals = repository.sumReviewCountByFreelancer(freelancerId);
        long rejections = repository.countRejectedByFreelancer(freelancerId);
        long suspensions = repository.countSuspendedByFreelancer(freelancerId);
        double renewalRate = total > 0 ? (double) totalRenewals / total : 0;

        double avgRating = 0;
        long reviewCount = 0;
        for (VendorApproval va : all) {
            try {
                ReviewStatsRemoteDto stats = reviewFeignClient.getPairStats(va.getOrganizationId(), freelancerId);
                if (stats != null && stats.getTotalCount() > 0) {
                    avgRating += stats.getAverageRating() * stats.getTotalCount();
                    reviewCount += stats.getTotalCount();
                }
            } catch (Exception e) {
                log.debug("[TRUST] Review unavailable for org={}: {}", va.getOrganizationId(), e.getMessage());
            }
        }
        if (reviewCount > 0) {
            avgRating = avgRating / reviewCount;
        }

        List<String> breakdown = new ArrayList<>();
        int score = 0;

        // Renewals: 25 pts
        int renewalPts = Math.min(25, (int) (renewalRate * 25));
        score += renewalPts;
        breakdown.add("Renouvellements : " + renewalPts + "/25 (taux=" + String.format("%.0f%%", renewalRate * 100) + ")");

        // Penalty-free history: 20 pts (minus 5 per rejection, 3 per suspension)
        int historyPts = Math.max(0, 20 - (int) (rejections * 5 + suspensions * 3));
        score += historyPts;
        breakdown.add("Historique : " + historyPts + "/20 (rejets=" + rejections + ", suspensions=" + suspensions + ")");

        // Reviews: 30 pts
        int reviewPts = reviewCount > 0 ? Math.min(30, (int) (avgRating / 5.0 * 30)) : 0;
        score += reviewPts;
        breakdown.add("Avis clients : " + reviewPts + "/30 (moyenne=" + String.format("%.1f", avgRating) + "/5, nb=" + reviewCount + ")");

        // Active agreements: 15 pts (5 per active, max 15)
        int activePts = Math.min(15, (int) (active * 5));
        score += activePts;
        breakdown.add("Agréments actifs : " + activePts + "/15 (" + active + " actif(s))");

        // Seniority: 10 pts (1pt per 3 months since first agreement, max 10)
        LocalDateTime oldest = all.stream()
                .map(VendorApproval::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        long monthsSinceFirst = ChronoUnit.MONTHS.between(oldest, LocalDateTime.now());
        int seniorityPts = Math.min(10, (int) (monthsSinceFirst / 3));
        score += seniorityPts;
        breakdown.add("Ancienneté : " + seniorityPts + "/10 (" + monthsSinceFirst + " mois)");

        score = Math.min(100, Math.max(0, score));
        String label = scoreLabelFor(score);

        return VendorTrustScoreResponse.builder()
                .freelancerId(freelancerId)
                .score(score)
                .label(label)
                .totalAgreements(total)
                .activeAgreements(active)
                .totalRenewals(totalRenewals)
                .renewalRate(renewalRate)
                .rejectionCount(rejections)
                .suspensionCount(suspensions)
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .breakdown(breakdown)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  MÉTIER 4 — Historique & audit / statistiques
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public long countApprovedForOrganization(Long organizationId) {
        return repository.countApprovedByOrganization(organizationId);
    }

    @Transactional(readOnly = true)
    public long countApprovedForFreelancer(Long freelancerId) {
        return repository.countApprovedByFreelancer(freelancerId);
    }

    @Transactional(readOnly = true)
    public List<VendorApprovalResponse> getByStatus(VendorApprovalStatus status) {
        return repository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VendorAuditEntryResponse> getAuditHistory(Long vendorApprovalId) {
        findOrThrow(vendorApprovalId);
        return auditRepository.findByVendorApprovalIdOrderByCreatedAtDesc(vendorApprovalId)
                .stream().map(this::toAuditResponse).collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════

    private String scoreLabelFor(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Bon";
        if (score >= 40) return "Moyen";
        if (score >= 20) return "Faible";
        return "Insuffisant";
    }

    private void appendAudit(Long vendorApprovalId, VendorApprovalStatus from, VendorApprovalStatus to,
                             String action, Long actorUserId, String detail) {
        VendorApprovalAudit row = new VendorApprovalAudit();
        row.setVendorApprovalId(vendorApprovalId);
        row.setFromStatus(from);
        row.setToStatus(to);
        row.setAction(action);
        row.setActorUserId(actorUserId);
        row.setDetail(detail);
        auditRepository.save(row);
    }

    private EligibilityDetailResponse evaluateEligibility(VendorApproval va, String requiredDomain) {
        if (va.getStatus() != VendorApprovalStatus.APPROVED) {
            return EligibilityDetailResponse.no("NOT_APPROVED",
                    "L'agrément n'est pas approuvé (statut : " + va.getStatus() + ").");
        }
        if (va.getValidUntil() != null && LocalDate.now().isAfter(va.getValidUntil())) {
            return EligibilityDetailResponse.no("EXPIRED_DATE", "La période de validité de l'agrément est dépassée.");
        }
        if (requiredDomain != null && !requiredDomain.isBlank()) {
            String d = va.getDomain();
            if (d == null || d.isBlank()) {
                return EligibilityDetailResponse.no("DOMAIN_MISSING",
                        "Un domaine est requis pour cette offre mais n'est pas renseigné sur l'agrément.");
            }
            if (!domainMatches(d, requiredDomain.trim())) {
                return EligibilityDetailResponse.no("DOMAIN_MISMATCH",
                        "Le domaine de l'agrément (« " + d + " ») ne correspond pas à l'exigence (« "
                                + requiredDomain.trim() + " »).");
            }
        }
        return EligibilityDetailResponse.ok();
    }

    private static boolean domainMatches(String agreementDomain, String required) {
        String a = agreementDomain.trim().toLowerCase();
        String r = required.trim().toLowerCase();
        if (a.isEmpty() || r.isEmpty()) return false;
        return a.contains(r) || r.contains(a);
    }

    private VendorAuditEntryResponse toAuditResponse(VendorApprovalAudit e) {
        VendorAuditEntryResponse r = new VendorAuditEntryResponse();
        r.setId(e.getId());
        r.setVendorApprovalId(e.getVendorApprovalId());
        r.setFromStatus(e.getFromStatus());
        r.setToStatus(e.getToStatus());
        r.setAction(e.getAction());
        r.setActorUserId(e.getActorUserId());
        r.setDetail(e.getDetail());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private String buildAutomaticCreationNotes(VendorApprovalRequest request) {
        String domainPart = (request.getDomain() != null && !request.getDomain().isBlank())
                ? request.getDomain().trim()
                : "non renseigné";
        String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return String.format(
                "Agrément créé automatiquement le %s — Organisation #%d, Freelancer #%d — Domaine : %s.",
                when,
                request.getOrganizationId(),
                request.getFreelancerId(),
                domainPart
        );
    }

    private VendorApproval findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agrément fournisseur introuvable : " + id));
    }

    private void assertTransition(VendorApproval va, VendorApprovalStatus target) {
        if (!va.canTransitionTo(target)) {
            throw new BadRequestException(
                    "Transition invalide : " + va.getStatus() + " → " + target
                            + ". Consultez le workflow d'agrément.");
        }
    }

    private VendorApprovalResponse toResponse(VendorApproval va) {
        VendorApprovalResponse r = new VendorApprovalResponse();
        r.setId(va.getId());
        r.setOrganizationId(va.getOrganizationId());
        r.setFreelancerId(va.getFreelancerId());
        r.setStatus(va.getStatus());
        r.setDomain(va.getDomain());
        r.setValidFrom(va.getValidFrom());
        r.setValidUntil(va.getValidUntil());
        r.setNextReviewDate(va.getNextReviewDate());
        r.setApprovedBy(va.getApprovedBy());
        r.setApprovalNotes(va.getApprovalNotes());
        r.setRejectionReason(va.getRejectionReason());
        r.setSuspensionReason(va.getSuspensionReason());
        r.setReviewCount(va.getReviewCount());
        r.setCreatedAt(va.getCreatedAt());
        r.setUpdatedAt(va.getUpdatedAt());
        r.setStatusChangedAt(va.getStatusChangedAt());
        r.setIsActive(va.isActiveAndValid());
        r.setIsReviewOverdue(va.isReviewOverdue());
        r.setIsReviewUpcoming(va.isReviewUpcoming());
        r.setClientSignedAt(va.getClientSignedAt());
        r.setClientSignerName(va.getClientSignerName());
        r.setFreelancerSignedAt(va.getFreelancerSignedAt());
        r.setFreelancerSignerName(va.getFreelancerSignerName());
        r.setFullySigned(va.getClientSignedAt() != null && va.getFreelancerSignedAt() != null);
        r.setProfessionalSector(va.getProfessionalSector());
        r.setExpiryReminderSentAt(va.getExpiryReminderSentAt());
        if (va.getValidUntil() != null) {
            r.setDaysUntilValidUntilExpiry(ChronoUnit.DAYS.between(LocalDate.now(), va.getValidUntil()));
        }
        return r;
    }
}
