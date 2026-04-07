package org.example.subcontracting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.ProjectFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.NotificationRequestDto;
import org.example.subcontracting.client.dto.ProjectRemoteDto;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.request.DeliverableRequest;
import org.example.subcontracting.dto.request.DeliverableReviewRequest;
import org.example.subcontracting.dto.request.DeliverableSubmitRequest;
import org.example.subcontracting.dto.request.SubcontractRequest;
import org.example.subcontracting.dto.response.DeliverableResponse;
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
    private final NotificationFeignClient notificationClient;
    private final SubcontractAuditService auditService;

    // ══════════════════════════════════════════════════════════
    //  CRUD — Subcontracts
    // ══════════════════════════════════════════════════════════

    public SubcontractResponse create(Long mainFreelancerId, SubcontractRequest req) {
        if (mainFreelancerId.equals(req.getSubcontractorId())) {
            throw new BadRequestException("Impossible de se sous-traiter soi-même");
        }

        Subcontract sc = new Subcontract();
        sc.setMainFreelancerId(mainFreelancerId);
        sc.setSubcontractorId(req.getSubcontractorId());
        sc.setProjectId(req.getProjectId());
        sc.setContractId(req.getContractId());
        sc.setTitle(req.getTitle());
        sc.setScope(req.getScope());
        sc.setCategory(SubcontractCategory.valueOf(req.getCategory().toUpperCase()));
        sc.setBudget(req.getBudget());
        sc.setCurrency(req.getCurrency() != null ? req.getCurrency() : "TND");
        sc.setStartDate(req.getStartDate());
        sc.setDeadline(req.getDeadline());
        sc.setStatus(SubcontractStatus.DRAFT);

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
        return toResponse(sc);
    }

    public SubcontractResponse accept(Long id) {
        Subcontract sc = findOrThrow(id);
        String from = sc.getStatus().name();
        transition(sc, SubcontractStatus.ACCEPTED);
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
        sc.setRejectionReason(reason);
        sc = subcontractRepo.save(sc);
        auditService.record(sc.getId(), sc.getSubcontractorId(), "REJECTED", from, "REJECTED",
                "Refusée : " + (reason != null ? reason : ""), null, null);
        notify(sc.getMainFreelancerId(), "SUBCONTRACT_REJECTED",
                "Sous-traitance refusée",
                "Le sous-traitant a refusé : " + sc.getTitle() + ". Raison : " + reason);
        return toResponse(sc);
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

    private void notify(Long userId, String type, String title, String message) {
        try {
            notificationClient.sendNotification(NotificationRequestDto.builder()
                    .userId(userId).type(type).title(title).message(message).build());
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

    private String safeProjectTitle(Long projectId) {
        try {
            ProjectRemoteDto p = projectClient.getProjectById(projectId);
            if (p != null && p.getTitle() != null) return p.getTitle();
        } catch (Exception e) {
            log.debug("[SUBCONTRACT] Project service unavailable: {}", e.getMessage());
        }
        return "Project #" + projectId;
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
                .projectTitle(safeProjectTitle(sc.getProjectId()))
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
