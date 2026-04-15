package org.example.subcontracting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.OfferApplicationFeignClient;
import org.example.subcontracting.client.OfferFeignClient;
import org.example.subcontracting.client.ProjectFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.NotificationRequestDto;
import org.example.subcontracting.client.dto.OfferApplicationRemoteDto;
import org.example.subcontracting.client.dto.OfferRemoteDto;
import org.example.subcontracting.client.dto.ProjectRemoteDto;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.request.DeliverableRequest;
import org.example.subcontracting.dto.request.DeliverableReviewRequest;
import org.example.subcontracting.dto.request.DeliverableSubmitRequest;
import org.example.subcontracting.dto.request.SubcontractRequest;
import org.example.subcontracting.dto.request.CounterOfferRequest;
import org.example.subcontracting.dto.request.AiMediateRequest;
import org.example.subcontracting.dto.response.DeliverableResponse;
import org.example.subcontracting.dto.response.NegotiationRoundResponse;
import org.example.subcontracting.dto.response.SubcontractResponse;
import org.example.subcontracting.entity.*;
import org.example.subcontracting.exception.BadRequestException;
import org.example.subcontracting.exception.ResourceNotFoundException;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubcontractService {

    private final SubcontractRepository subcontractRepo;
    private final SubcontractDeliverableRepository deliverableRepo;
    private final UserFeignClient userClient;
    private final ProjectFeignClient projectClient;
    private final OfferApplicationFeignClient offerApplicationClient;
    private final OfferFeignClient offerClient;
    private final NotificationFeignClient notificationClient;
    private final SubcontractAuditService auditService;
    private final SubcontractEmailService subcontractEmailService;
    private final SubcontractCoachingService coachingService;
    private final ObjectMapper objectMapper;

    // ══════════════════════════════════════════════════════════
    //  CRUD — Subcontracts
    // ══════════════════════════════════════════════════════════

    public SubcontractResponse create(Long mainFreelancerId, SubcontractRequest req) {
        if (mainFreelancerId.equals(req.getSubcontractorId())) {
            throw new BadRequestException("Impossible de se sous-traiter soi-même");
        }

        Long pid = req.getProjectId();
        Long oid = req.getOfferId();
        boolean hasP = pid != null && pid > 0;
        boolean hasO = oid != null && oid > 0;
        if (hasP == hasO) {
            throw new BadRequestException(
                    "Indiquez exactement une mission : soit projectId (projet), soit offerId (offre avec candidature acceptée).");
        }
        if (hasO) {
            assertOfferAcceptedForMainFreelancer(mainFreelancerId, oid);
        }

        Subcontract sc = new Subcontract();
        sc.setMainFreelancerId(mainFreelancerId);
        sc.setSubcontractorId(req.getSubcontractorId());
        if (hasP) {
            sc.setProjectId(pid);
            sc.setOfferId(null);
        } else {
            sc.setProjectId(null);
            sc.setOfferId(oid);
        }
        sc.setContractId(req.getContractId());
        sc.setTitle(req.getTitle());
        sc.setScope(req.getScope());
        sc.setCategory(SubcontractCategory.valueOf(req.getCategory().toUpperCase()));
        sc.setBudget(req.getBudget());
        sc.setCurrency(req.getCurrency() != null ? req.getCurrency() : "TND");
        sc.setStartDate(req.getStartDate());
        sc.setDeadline(req.getDeadline());
        sc.setStatus(SubcontractStatus.DRAFT);
        sc.setRequiredSkillsJson(skillsToJson(req.getRequiredSkills()));
        applyMediaFromRequest(sc, req);

        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), mainFreelancerId, "CREATED", null, "DRAFT",
                "Sous-traitance créée : " + sc.getTitle(), null, null);
        log.info("[SUBCONTRACT] Created #{} by freelancer={}", sc.getId(), mainFreelancerId);
        return toResponse(sc);
    }

    @Transactional(readOnly = true)
    public SubcontractResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<SubcontractResponse> getByMainFreelancer(Long freelancerId) {
        return subcontractRepo.findByMainFreelancerIdOrderByCreatedAtDesc(freelancerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubcontractResponse> getBySubcontractor(Long subcontractorId) {
        return subcontractRepo.findBySubcontractorIdOrderByCreatedAtDesc(subcontractorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubcontractResponse> getByProject(Long projectId) {
        return subcontractRepo.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubcontractResponse> getByStatus(String status) {
        SubcontractStatus s = SubcontractStatus.valueOf(status.toUpperCase());
        return subcontractRepo.findByStatusOrderByCreatedAtDesc(s)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubcontractResponse> getAll() {
        return subcontractRepo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public SubcontractResponse update(Long id, SubcontractRequest req) {
        Subcontract sc = findOrThrow(id);
        if (sc.getStatus() != SubcontractStatus.DRAFT) {
            throw new BadRequestException("Seul un brouillon (DRAFT) peut être modifié");
        }
        sc.setTitle(req.getTitle());
        sc.setScope(req.getScope());
        sc.setCategory(SubcontractCategory.valueOf(req.getCategory().toUpperCase()));
        sc.setBudget(req.getBudget());
        if (req.getCurrency() != null) sc.setCurrency(req.getCurrency());
        sc.setStartDate(req.getStartDate());
        sc.setDeadline(req.getDeadline());
        sc.setRequiredSkillsJson(skillsToJson(req.getRequiredSkills()));
        applyMediaFromRequest(sc, req);
        return toResponse(subcontractRepo.save(sc));
    }

    public void delete(Long id) {
        Subcontract sc = findOrThrow(id);
        if (sc.getStatus() != SubcontractStatus.DRAFT && sc.getStatus() != SubcontractStatus.CANCELLED) {
            throw new BadRequestException("Seul un brouillon ou une sous-traitance annulée peut être supprimée");
        }
        subcontractRepo.delete(sc);
        log.info("[SUBCONTRACT] Deleted #{}", id);
    }

    // ══════════════════════════════════════════════════════════
    //  Workflow transitions
    // ══════════════════════════════════════════════════════════

    public SubcontractResponse propose(Long id) {
        Subcontract sc = findOrThrow(id);
        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.PROPOSED);
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getMainFreelancerId(), "PROPOSED", from, "PROPOSED",
                "Proposition envoyée au sous-traitant", null, null);
        notify(sc.getSubcontractorId(), "SUBCONTRACT_PROPOSED",
                "Nouvelle proposition de sous-traitance",
                "Vous avez reçu une proposition de sous-traitance : " + sc.getTitle());
        subcontractEmailService.sendProposedEmail(sc);
        return toResponse(sc);
    }

    public SubcontractResponse accept(Long id) {
        Subcontract sc = findOrThrow(id);
        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.ACCEPTED);
        sc.setNegotiationStatus("ACCEPTED");
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getSubcontractorId(), "ACCEPTED", from, "ACCEPTED",
                "Proposition acceptée par le sous-traitant", null, null);
        notify(sc.getMainFreelancerId(), "SUBCONTRACT_ACCEPTED",
                "Sous-traitance acceptée",
                "Le sous-traitant a accepté : " + sc.getTitle());
        return toResponse(sc);
    }

    public SubcontractResponse reject(Long id, String reason) {
        Subcontract sc = findOrThrow(id);
        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.REJECTED);
        sc.setNegotiationStatus("REJECTED");
        sc.setRejectionReason(reason);
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getSubcontractorId(), "REJECTED", from, "REJECTED",
                "Refusée : " + (reason != null ? reason : ""), null, null);
        notify(sc.getMainFreelancerId(), "SUBCONTRACT_REJECTED",
                "Sous-traitance refusée",
                "Le sous-traitant a refusé : " + sc.getTitle() + ". Raison : " + reason);
        return toResponse(sc);
    }

    public NegotiationRoundResponse counterOffer(Long id, Long subcontractorId, CounterOfferRequest req) {
        Subcontract sc = findOrThrow(id);
        if (subcontractorId != null && !subcontractorId.equals(sc.getSubcontractorId())) {
            throw new BadRequestException("Seul le sous-traitant assigné peut proposer une contre-offre.");
        }
        if (sc.getNegotiationRoundCount() != null && sc.getNegotiationRoundCount() >= 3) {
            forceNegotiationImpasse(sc);
            throw new BadRequestException("Maximum 3 rounds atteint. L'IA déclare une impasse.");
        }
        if (!(sc.getStatus() == SubcontractStatus.PROPOSED
                || sc.getStatus() == SubcontractStatus.AI_MEDIATION
                || sc.getStatus() == SubcontractStatus.NEGOTIATED
                || sc.getStatus() == SubcontractStatus.COUNTER_OFFERED)) {
            throw new BadRequestException("Contre-offre non autorisée dans l'état " + sc.getStatus());
        }

        int nextRound = (sc.getNegotiationRoundCount() == null ? 0 : sc.getNegotiationRoundCount()) + 1;
        if (nextRound > 3) {
            forceNegotiationImpasse(sc);
            throw new BadRequestException("Maximum 3 rounds atteint. L'IA déclare une impasse.");
        }

        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.COUNTER_OFFERED);
        sc.setNegotiationRoundCount(nextRound);
        sc.setNegotiationStatus("COUNTER_OFFERED");
        sc.setCounterOfferBudget(req.getProposedBudget());
        sc.setCounterOfferDurationDays(req.getProposedDurationDays());
        sc.setCounterOfferNote(req.getNote());
        sc.setAiCompromiseBudget(null);
        sc.setAiCompromiseDurationDays(null);
        sc.setAiCompromiseJustification(null);
        sc = subcontractRepo.save(sc);

        String detail = "Round " + nextRound
                + " | primary={budget=" + safeAmount(sc.getBudget()) + ", durationDays=" + computeDurationDays(sc) + "}"
                + " | counter={budget=" + safeAmount(req.getProposedBudget()) + ", durationDays=" + req.getProposedDurationDays() + "}"
                + (req.getNote() != null && !req.getNote().isBlank() ? " | note=" + req.getNote() : "");
        auditService.record(sc.getId(), sc.getSubcontractorId(), "COUNTER_OFFERED", from, "COUNTER_OFFERED",
                detail, "NEGOTIATION_ROUND", (long) nextRound);

        notify(sc.getMainFreelancerId(), "SUBCONTRACT_COUNTER_OFFERED",
                "Contre-offre reçue",
                "Le sous-traitant a proposé une contre-offre sur '" + sc.getTitle() + "'.");

        return buildNegotiationRoundResponse(sc);
    }

    public NegotiationRoundResponse aiMediate(Long id, Long mainFreelancerId, AiMediateRequest req) {
        Subcontract sc = findOrThrow(id);
        if (mainFreelancerId != null && !mainFreelancerId.equals(sc.getMainFreelancerId())) {
            throw new BadRequestException("Seul le freelancer principal peut lancer la médiation IA.");
        }
        if (sc.getStatus() != SubcontractStatus.COUNTER_OFFERED) {
            throw new BadRequestException("Médiation IA disponible uniquement après une contre-offre.");
        }
        int round = sc.getNegotiationRoundCount() == null ? 0 : sc.getNegotiationRoundCount();
        if (round <= 0) {
            throw new BadRequestException("Aucun round de négociation en cours.");
        }

        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.AI_MEDIATION);

        Integer primaryDuration = computeDurationDays(sc);
        Integer counterDuration = sc.getCounterOfferDurationDays() != null ? sc.getCounterOfferDurationDays() : primaryDuration;
        BigDecimal primaryBudget = safeAmount(sc.getBudget());
        BigDecimal counterBudget = safeAmount(sc.getCounterOfferBudget() != null ? sc.getCounterOfferBudget() : sc.getBudget());

        BigDecimal marketDailyRate = estimateMarketDailyRate(sc);
        int targetDuration = averageInt(primaryDuration, counterDuration);
        BigDecimal marketAnchorBudget = marketDailyRate.multiply(BigDecimal.valueOf(Math.max(1, targetDuration)));

        BigDecimal aiBudget = weightedCompromise(primaryBudget, counterBudget, marketAnchorBudget);
        int aiDuration = weightedDuration(primaryDuration, counterDuration);

        BigDecimal budgetGapRatio = percentGap(primaryBudget, counterBudget);
        boolean impasse = round >= 3 && budgetGapRatio.compareTo(BigDecimal.valueOf(0.35)) > 0;

        String justification = buildCompromiseJustification(primaryBudget, counterBudget, aiBudget,
                primaryDuration, counterDuration, aiDuration, marketDailyRate, req != null ? req.getNote() : null, impasse);

        sc.setAiCompromiseBudget(aiBudget);
        sc.setAiCompromiseDurationDays(aiDuration);
        sc.setAiCompromiseJustification(justification);
        sc.setNegotiationStatus(impasse ? "NEGOTIATION_IMPASSE" : "NEGOTIATED");
        if (impasse) {
            transition(sc, SubcontractStatus.NEGOTIATION_IMPASSE);
        } else {
            transition(sc, SubcontractStatus.NEGOTIATED);
        }
        sc = subcontractRepo.save(sc);

        String detail = "Round " + round
                + " | primary={budget=" + primaryBudget + ", durationDays=" + primaryDuration + "}"
                + " | counter={budget=" + counterBudget + ", durationDays=" + counterDuration + "}"
                + " | ai={budget=" + aiBudget + ", durationDays=" + aiDuration + "}"
                + " | status=" + sc.getNegotiationStatus();
        auditService.record(sc.getId(), sc.getMainFreelancerId(), "AI_MEDIATED", from, sc.getStatus().name(),
                detail, "NEGOTIATION_ROUND", (long) round);

        String notifTitle = impasse ? "Médiation IA : impasse" : "Médiation IA : compromis proposé";
        String notifBody = impasse
                ? "L'IA recommande d'abandonner ou de rechercher un autre sous-traitant pour '" + sc.getTitle() + "'."
                : "Un compromis IA est disponible pour '" + sc.getTitle() + "'.";
        notify(sc.getMainFreelancerId(), "SUBCONTRACT_AI_MEDIATION", notifTitle, notifBody);
        notify(sc.getSubcontractorId(), "SUBCONTRACT_AI_MEDIATION", notifTitle, notifBody);

        return buildNegotiationRoundResponse(sc);
    }

    public SubcontractResponse startWork(Long id) {
        Subcontract sc = findOrThrow(id);
        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.IN_PROGRESS);
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getMainFreelancerId(), "STARTED", from, "IN_PROGRESS",
                "Travail démarré", null, null);
        notify(sc.getSubcontractorId(), "SUBCONTRACT_STARTED",
                "Travail démarré",
                "La sous-traitance '" + sc.getTitle() + "' est maintenant IN_PROGRESS");
        return toResponse(sc);
    }

    public SubcontractResponse complete(Long id) {
        Subcontract sc = findOrThrow(id);
        long pending = deliverableRepo.countBySubcontractIdAndStatus(id, DeliverableStatus.PENDING)
                + deliverableRepo.countBySubcontractIdAndStatus(id, DeliverableStatus.IN_PROGRESS)
                + deliverableRepo.countBySubcontractIdAndStatus(id, DeliverableStatus.SUBMITTED);
        if (pending > 0) {
            throw new BadRequestException("Tous les livrables doivent être validés avant de compléter la sous-traitance (" + pending + " en attente)");
        }
        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.COMPLETED);
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getMainFreelancerId(), "COMPLETED", from, "COMPLETED",
                "Tous les livrables validés, sous-traitance complétée", null, null);
        notify(sc.getMainFreelancerId(), "SUBCONTRACT_COMPLETED",
                "Sous-traitance terminée",
                "Tous les livrables de '" + sc.getTitle() + "' sont validés. Sous-traitance complétée.");
        return toResponse(sc);
    }

    public SubcontractResponse cancel(Long id, String reason) {
        Subcontract sc = findOrThrow(id);
        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.CANCELLED);
        sc.setCancellationReason(reason);
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getMainFreelancerId(), "CANCELLED", from, "CANCELLED",
                "Annulée : " + (reason != null ? reason : ""), null, null);
        notify(sc.getSubcontractorId(), "SUBCONTRACT_CANCELLED",
                "Sous-traitance annulée",
                "La sous-traitance '" + sc.getTitle() + "' a été annulée. Raison : " + reason);
        return toResponse(sc);
    }

    public SubcontractResponse close(Long id) {
        Subcontract sc = findOrThrow(id);
        transition(sc, SubcontractStatus.CLOSED);
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getMainFreelancerId(), "CLOSED", "COMPLETED", "CLOSED",
                "Sous-traitance clôturée", null, null);
        coachingService.refreshProfile(sc.getMainFreelancerId());
        return toResponse(sc);
    }

    public SubcontractResponse reopen(Long id) {
        Subcontract sc = findOrThrow(id);
        transition(sc, SubcontractStatus.DRAFT);
        sc.setRejectionReason(null);
        sc = subcontractRepo.save(sc);
        return toResponse(sc);
    }

    // ══════════════════════════════════════════════════════════
    //  CRUD — Deliverables
    // ══════════════════════════════════════════════════════════

    public DeliverableResponse addDeliverable(Long subcontractId, DeliverableRequest req) {
        Subcontract sc = findOrThrow(subcontractId);
        SubcontractDeliverable d = new SubcontractDeliverable();
        d.setSubcontract(sc);
        d.setTitle(req.getTitle());
        d.setDescription(req.getDescription());
        d.setDeadline(req.getDeadline());
        d.setStatus(DeliverableStatus.PENDING);
        d = deliverableRepo.save(d);
        auditService.record(subcontractId, sc.getMainFreelancerId(), "DELIVERABLE_ADDED", null, null,
                "Livrable ajouté : " + d.getTitle(), "DELIVERABLE", d.getId());
        log.info("[DELIVERABLE] Added #{} to subcontract #{}", d.getId(), subcontractId);
        return toDeliverableResponse(d);
    }

    @Transactional(readOnly = true)
    public List<DeliverableResponse> getDeliverables(Long subcontractId) {
        return deliverableRepo.findBySubcontractIdOrderByDeadlineAsc(subcontractId)
                .stream().map(this::toDeliverableResponse).collect(Collectors.toList());
    }

    public DeliverableResponse updateDeliverable(Long deliverableId, DeliverableRequest req) {
        SubcontractDeliverable d = findDeliverableOrThrow(deliverableId);
        if (d.getStatus() != DeliverableStatus.PENDING) {
            throw new BadRequestException("Seul un livrable PENDING peut être modifié");
        }
        d.setTitle(req.getTitle());
        d.setDescription(req.getDescription());
        d.setDeadline(req.getDeadline());
        return toDeliverableResponse(deliverableRepo.save(d));
    }

    public void deleteDeliverable(Long deliverableId) {
        SubcontractDeliverable d = findDeliverableOrThrow(deliverableId);
        if (d.getStatus() != DeliverableStatus.PENDING) {
            throw new BadRequestException("Seul un livrable PENDING peut être supprimé");
        }
        deliverableRepo.delete(d);
        log.info("[DELIVERABLE] Deleted #{}", deliverableId);
    }

    /** Sous-traitant soumet un livrable. */
    public DeliverableResponse submitDeliverable(Long deliverableId, DeliverableSubmitRequest req) {
        SubcontractDeliverable d = findDeliverableOrThrow(deliverableId);
        if (d.getStatus() != DeliverableStatus.PENDING && d.getStatus() != DeliverableStatus.IN_PROGRESS
                && d.getStatus() != DeliverableStatus.REJECTED) {
            throw new BadRequestException("Ce livrable ne peut pas être soumis dans son état actuel : " + d.getStatus());
        }
        d.setStatus(DeliverableStatus.SUBMITTED);
        d.setSubmissionUrl(req.getSubmissionUrl());
        d.setSubmissionNote(req.getSubmissionNote());
        d.setSubmittedAt(LocalDateTime.now());
        d = deliverableRepo.save(d);
        auditService.record(d.getSubcontract().getId(), d.getSubcontract().getSubcontractorId(),
                "DELIVERABLE_SUBMITTED", null, "SUBMITTED",
                "Livrable soumis : " + d.getTitle(), "DELIVERABLE", d.getId());

        notify(d.getSubcontract().getMainFreelancerId(), "DELIVERABLE_SUBMITTED",
                "Livrable soumis",
                "Le livrable '" + d.getTitle() + "' a été soumis pour validation.");
        return toDeliverableResponse(d);
    }

    /** Freelancer principal valide ou rejette un livrable. */
    public DeliverableResponse reviewDeliverable(Long deliverableId, DeliverableReviewRequest req) {
        SubcontractDeliverable d = findDeliverableOrThrow(deliverableId);
        if (d.getStatus() != DeliverableStatus.SUBMITTED) {
            throw new BadRequestException("Seul un livrable SUBMITTED peut être validé/rejeté");
        }
        d.setStatus(Boolean.TRUE.equals(req.getApproved()) ? DeliverableStatus.APPROVED : DeliverableStatus.REJECTED);
        d.setReviewNote(req.getReviewNote());
        d.setReviewedAt(LocalDateTime.now());
        d = deliverableRepo.save(d);
        String auditAction = d.getStatus() == DeliverableStatus.APPROVED ? "DELIVERABLE_APPROVED" : "DELIVERABLE_REJECTED";
        auditService.record(d.getSubcontract().getId(), d.getSubcontract().getMainFreelancerId(),
                auditAction, "SUBMITTED", d.getStatus().name(),
                "Livrable " + (d.getStatus() == DeliverableStatus.APPROVED ? "approuvé" : "rejeté") + " : " + d.getTitle(),
                "DELIVERABLE", d.getId());

        String action = d.getStatus() == DeliverableStatus.APPROVED ? "approuvé" : "rejeté";
        notify(d.getSubcontract().getSubcontractorId(), "DELIVERABLE_REVIEWED",
                "Livrable " + action,
                "Le livrable '" + d.getTitle() + "' a été " + action + ".");
        return toDeliverableResponse(d);
    }

    // ══════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════

    private Subcontract findOrThrow(Long id) {
        return subcontractRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sous-traitance introuvable : " + id));
    }

    private SubcontractDeliverable findDeliverableOrThrow(Long id) {
        return deliverableRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livrable introuvable : " + id));
    }

    private void transition(Subcontract sc, SubcontractStatus target) {
        if (!sc.canTransitionTo(target)) {
            throw new BadRequestException("Transition interdite : " + sc.getStatus() + " → " + target);
        }
        sc.setStatus(target);
        sc.setStatusChangedAt(LocalDateTime.now());
    }

    private void forceNegotiationImpasse(Subcontract sc) {
        sc.setNegotiationStatus("NEGOTIATION_IMPASSE");
        if (sc.canTransitionTo(SubcontractStatus.NEGOTIATION_IMPASSE)) {
            sc.setStatus(SubcontractStatus.NEGOTIATION_IMPASSE);
            sc.setStatusChangedAt(LocalDateTime.now());
        }
        subcontractRepo.save(sc);
    }

    private NegotiationRoundResponse buildNegotiationRoundResponse(Subcontract sc) {
        return NegotiationRoundResponse.builder()
                .roundNumber(sc.getNegotiationRoundCount() == null ? 0 : sc.getNegotiationRoundCount())
                .negotiationStatus(sc.getNegotiationStatus())
                .primaryOffer(NegotiationRoundResponse.OfferPosition.builder()
                        .budget(safeAmount(sc.getBudget()))
                        .durationDays(computeDurationDays(sc))
                        .build())
                .subcontractorOffer(NegotiationRoundResponse.OfferPosition.builder()
                        .budget(sc.getCounterOfferBudget())
                        .durationDays(sc.getCounterOfferDurationDays())
                        .build())
                .aiCompromise(sc.getAiCompromiseBudget() == null && sc.getAiCompromiseDurationDays() == null
                        ? null
                        : NegotiationRoundResponse.OfferPosition.builder()
                        .budget(sc.getAiCompromiseBudget())
                        .durationDays(sc.getAiCompromiseDurationDays())
                        .build())
                .compromiseJustification(sc.getAiCompromiseJustification())
                .build();
    }

    private Integer computeDurationDays(Subcontract sc) {
        if (sc.getStartDate() == null || sc.getDeadline() == null) return null;
        long days = ChronoUnit.DAYS.between(sc.getStartDate(), sc.getDeadline());
        return (int) Math.max(1, days);
    }

    private BigDecimal estimateMarketDailyRate(Subcontract sc) {
        List<Subcontract> benchmarks = subcontractRepo.findByCategoryAndStatusIn(
                sc.getCategory(),
                EnumSet.of(SubcontractStatus.COMPLETED, SubcontractStatus.CLOSED, SubcontractStatus.ACCEPTED));
        List<BigDecimal> dailyRates = benchmarks.stream()
                .map(x -> {
                    Integer d = computeDurationDays(x);
                    if (d == null || d <= 0 || x.getBudget() == null) return null;
                    return x.getBudget().divide(BigDecimal.valueOf(d), 6, RoundingMode.HALF_UP);
                })
                .filter(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (!dailyRates.isEmpty()) {
            BigDecimal sum = dailyRates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(BigDecimal.valueOf(dailyRates.size()), 6, RoundingMode.HALF_UP);
        }
        Integer ownDuration = computeDurationDays(sc);
        if (ownDuration != null && ownDuration > 0 && sc.getBudget() != null) {
            return sc.getBudget().divide(BigDecimal.valueOf(ownDuration), 6, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(120);
    }

    private BigDecimal weightedCompromise(BigDecimal primary, BigDecimal counter, BigDecimal market) {
        BigDecimal out = primary.multiply(BigDecimal.valueOf(0.35))
                .add(counter.multiply(BigDecimal.valueOf(0.35)))
                .add(market.multiply(BigDecimal.valueOf(0.30)));
        return out.setScale(2, RoundingMode.HALF_UP);
    }

    private int weightedDuration(Integer primary, Integer counter) {
        int p = primary != null ? primary : 14;
        int c = counter != null ? counter : p;
        return (int) Math.max(1, Math.round((p * 0.55f) + (c * 0.45f)));
    }

    private int averageInt(Integer a, Integer b) {
        int av = a != null ? a : 14;
        int bv = b != null ? b : av;
        return Math.max(1, Math.round((av + bv) / 2f));
    }

    private BigDecimal percentGap(BigDecimal a, BigDecimal b) {
        BigDecimal max = a.max(b);
        if (max.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return a.subtract(b).abs().divide(max, 6, RoundingMode.HALF_UP);
    }

    private String buildCompromiseJustification(
            BigDecimal primaryBudget,
            BigDecimal counterBudget,
            BigDecimal aiBudget,
            Integer primaryDuration,
            Integer counterDuration,
            Integer aiDuration,
            BigDecimal marketDailyRate,
            String note,
            boolean impasse
    ) {
        String base = "Compromis calculé sur un équilibre 35% offre initiale, 35% contre-offre, 30% référence marché. "
                + "Référence marché estimée: "
                + marketDailyRate.setScale(2, RoundingMode.HALF_UP) + " /jour. "
                + "Proposition IA: budget " + aiBudget + " et durée " + aiDuration + " jours. "
                + "Positions analysées: principal(" + primaryBudget + ", " + primaryDuration + "j) vs sous-traitant("
                + counterBudget + ", " + counterDuration + "j).";
        if (note != null && !note.isBlank()) {
            base += " Note médiation: " + note + ".";
        }
        if (impasse) {
            base += " Après 3 rounds, l'écart reste trop élevé. Recommandation: abandonner la négociation ou sélectionner un autre sous-traitant.";
        }
        return base;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void applyMediaFromRequest(Subcontract sc, SubcontractRequest req) {
        if (req.getMediaUrl() == null || req.getMediaUrl().isBlank()) {
            sc.setMediaUrl(null);
            sc.setMediaType(null);
            return;
        }
        sc.setMediaUrl(req.getMediaUrl().trim());
        if (req.getMediaType() == null || req.getMediaType().isBlank()) {
            sc.setMediaType(null);
        } else {
            sc.setMediaType(SubcontractMediaType.valueOf(req.getMediaType().trim().toUpperCase()));
        }
    }

    private String skillsToJson(List<String> skills) {
        if (skills == null || skills.isEmpty()) return null;
        List<String> cleaned = skills.stream()
                .filter(s -> s != null)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (cleaned.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseSkillsJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void notify(Long userId, String type, String title, String message) {
        try {
            notificationClient.sendNotification(NotificationRequestDto.builder()
                    .userId(String.valueOf(userId)).type(type).title(title).body(message).build());
        } catch (Exception e) {
            log.warn("[SUBCONTRACT] Notification failed for user={}: {}", userId, e.getMessage());
        }
    }

    private String safeUserName(Long userId) {
        try {
            UserRemoteDto u = userClient.getUserById(userId);
            if (u != null) {
                String name = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                        + (u.getLastName() != null ? u.getLastName() : "")).trim();
                return name.isBlank() ? "User #" + userId : name;
            }
        } catch (Exception e) {
            log.debug("[SUBCONTRACT] User service unavailable: {}", e.getMessage());
        }
        return "User #" + userId;
    }

    private void assertOfferAcceptedForMainFreelancer(Long mainFreelancerId, Long offerId) {
        try {
            List<OfferApplicationRemoteDto> list =
                    offerApplicationClient.listAcceptedForFreelancerOwnedOffers(mainFreelancerId);
            boolean ok = list != null && list.stream().anyMatch(a -> offerId.equals(a.getOfferId()));
            if (!ok) {
                throw new BadRequestException(
                        "Offre non éligible : aucune candidature acceptée du client sur cette offre pour votre compte.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[SUBCONTRACT] Offer application validation failed: {}", e.getMessage());
            throw new BadRequestException(
                    "Impossible de valider l'offre (microservice Offer indisponible ou erreur réseau).");
        }
    }

    private String safeProjectTitle(Long projectId) {
        if (projectId == null) {
            return null;
        }
        try {
            ProjectRemoteDto p = projectClient.getProjectById(projectId);
            if (p != null && p.getTitle() != null) return p.getTitle();
        } catch (Exception e) {
            log.debug("[SUBCONTRACT] Project service unavailable: {}", e.getMessage());
        }
        return "Project #" + projectId;
    }

    private String safeOfferTitle(Long offerId) {
        if (offerId == null) {
            return null;
        }
        try {
            OfferRemoteDto o = offerClient.getOfferById(offerId);
            if (o != null && o.getTitle() != null) return o.getTitle();
        } catch (Exception e) {
            log.debug("[SUBCONTRACT] Offer service unavailable: {}", e.getMessage());
        }
        return "Offer #" + offerId;
    }

    private String resolveMissionTitle(Subcontract sc) {
        if (sc.getOfferId() != null) {
            return safeOfferTitle(sc.getOfferId());
        }
        if (sc.getProjectId() != null) {
            return safeProjectTitle(sc.getProjectId());
        }
        return "—";
    }

    // ══════════════════════════════════════════════════════════
    //  Mappers
    // ══════════════════════════════════════════════════════════

    private SubcontractResponse toResponse(Subcontract sc) {
        long total = deliverableRepo.countBySubcontractId(sc.getId());
        long approved = deliverableRepo.countBySubcontractIdAndStatus(sc.getId(), DeliverableStatus.APPROVED);
        long pending = total - approved;

        List<DeliverableResponse> delResponses = deliverableRepo
                .findBySubcontractIdOrderByDeadlineAsc(sc.getId())
                .stream().map(this::toDeliverableResponse).collect(Collectors.toList());

        return SubcontractResponse.builder()
                .id(sc.getId())
                .mainFreelancerId(sc.getMainFreelancerId())
                .mainFreelancerName(safeUserName(sc.getMainFreelancerId()))
                .subcontractorId(sc.getSubcontractorId())
                .subcontractorName(safeUserName(sc.getSubcontractorId()))
                .projectId(sc.getProjectId())
                .offerId(sc.getOfferId())
                .projectTitle(resolveMissionTitle(sc))
                .contractId(sc.getContractId())
                .title(sc.getTitle())
                .scope(sc.getScope())
                .category(sc.getCategory().name())
                .budget(sc.getBudget())
                .currency(sc.getCurrency())
                .status(sc.getStatus().name())
                .startDate(sc.getStartDate())
                .deadline(sc.getDeadline())
                .rejectionReason(sc.getRejectionReason())
                .cancellationReason(sc.getCancellationReason())
                .negotiationRoundCount(sc.getNegotiationRoundCount())
                .negotiationStatus(sc.getNegotiationStatus())
                .requiredSkills(parseSkillsJson(sc.getRequiredSkillsJson()))
                .mediaUrl(sc.getMediaUrl())
                .mediaType(sc.getMediaType() != null ? sc.getMediaType().name() : null)
                .createdAt(sc.getCreatedAt())
                .updatedAt(sc.getUpdatedAt())
                .statusChangedAt(sc.getStatusChangedAt())
                .totalDeliverables((int) total)
                .approvedDeliverables((int) approved)
                .pendingDeliverables((int) pending)
                .deliverables(delResponses)
                .build();
    }

    private DeliverableResponse toDeliverableResponse(SubcontractDeliverable d) {
        boolean overdue = d.getDeadline() != null
                && LocalDate.now().isAfter(d.getDeadline())
                && d.getStatus() != DeliverableStatus.APPROVED;
        return DeliverableResponse.builder()
                .id(d.getId())
                .subcontractId(d.getSubcontract().getId())
                .title(d.getTitle())
                .description(d.getDescription())
                .status(d.getStatus().name())
                .deadline(d.getDeadline())
                .submissionUrl(d.getSubmissionUrl())
                .submissionNote(d.getSubmissionNote())
                .submittedAt(d.getSubmittedAt())
                .reviewNote(d.getReviewNote())
                .reviewedAt(d.getReviewedAt())
                .createdAt(d.getCreatedAt())
                .overdue(overdue)
                .build();
    }
}
