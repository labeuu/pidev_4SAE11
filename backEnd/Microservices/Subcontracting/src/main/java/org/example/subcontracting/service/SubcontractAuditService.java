package org.example.subcontracting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.AuditTimelineEntry;
import org.example.subcontracting.dto.response.FreelancerHistoryResponse;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractAudit;
import org.example.subcontracting.repository.SubcontractAuditRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubcontractAuditService {

    private final SubcontractAuditRepository auditRepo;
    private final SubcontractRepository subcontractRepo;
    private final UserFeignClient userClient;

    /**
     * Enregistre un événement d'audit.
     */
    @Transactional
    public void record(Long subcontractId, Long actorUserId, String action,
                       String fromStatus, String toStatus, String detail,
                       String targetEntity, Long targetEntityId) {
        auditRepo.save(SubcontractAudit.builder()
                .subcontractId(subcontractId)
                .actorUserId(actorUserId)
                .action(action)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .detail(detail)
                .targetEntity(targetEntity)
                .targetEntityId(targetEntityId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Historique complet d'une sous-traitance.
     */
    @Transactional(readOnly = true)
    public List<AuditTimelineEntry> getBySubcontract(Long subcontractId) {
        return auditRepo.findBySubcontractIdOrderByCreatedAtDesc(subcontractId)
                .stream().map(this::toEntry).collect(Collectors.toList());
    }

    /**
     * MÉTIER 5 — Historique complet d'un freelancer (toutes ses sous-traitances).
     */
    @Transactional(readOnly = true)
    public FreelancerHistoryResponse getFreelancerHistory(Long userId) {
        List<SubcontractAudit> audits = auditRepo.findAllByFreelancerInvolved(userId);

        Map<String, Long> byAction = audits.stream()
                .collect(Collectors.groupingBy(SubcontractAudit::getAction, Collectors.counting()));

        long asMain = subcontractRepo.countByMainFreelancerId(userId);
        long asSub = subcontractRepo.countBySubcontractorId(userId);

        List<AuditTimelineEntry> timeline = audits.stream()
                .map(this::toEntry)
                .collect(Collectors.toList());

        return FreelancerHistoryResponse.builder()
                .userId(userId)
                .userName(safeUserName(userId))
                .totalEvents(audits.size())
                .eventsByAction(byAction)
                .asMainFreelancer(asMain)
                .asSubcontractor(asSub)
                .timeline(timeline)
                .build();
    }

    private AuditTimelineEntry toEntry(SubcontractAudit a) {
        String scTitle = subcontractRepo.findById(a.getSubcontractId())
                .map(Subcontract::getTitle).orElse("Sous-traitance #" + a.getSubcontractId());

        return AuditTimelineEntry.builder()
                .id(a.getId())
                .subcontractId(a.getSubcontractId())
                .subcontractTitle(scTitle)
                .action(a.getAction())
                .actionLabel(actionLabel(a.getAction()))
                .fromStatus(a.getFromStatus())
                .toStatus(a.getToStatus())
                .detail(a.getDetail())
                .targetEntity(a.getTargetEntity())
                .targetEntityId(a.getTargetEntityId())
                .actorUserId(a.getActorUserId())
                .actorName(a.getActorUserId() != null ? safeUserName(a.getActorUserId()) : "Système")
                .createdAt(a.getCreatedAt())
                .icon(actionIcon(a.getAction()))
                .color(actionColor(a.getAction()))
                .build();
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "CREATED" -> "Sous-traitance créée";
            case "STATUS_CHANGED" -> "Changement de statut";
            case "PROPOSED" -> "Proposition envoyée";
            case "ACCEPTED" -> "Proposition acceptée";
            case "REJECTED" -> "Proposition refusée";
            case "STARTED" -> "Travail démarré";
            case "COMPLETED" -> "Travail complété";
            case "CANCELLED" -> "Sous-traitance annulée";
            case "CLOSED" -> "Sous-traitance clôturée";
            case "REOPENED" -> "Sous-traitance rouverte";
            case "UPDATED" -> "Sous-traitance modifiée";
            case "DELETED" -> "Sous-traitance supprimée";
            case "DELIVERABLE_ADDED" -> "Livrable ajouté";
            case "DELIVERABLE_SUBMITTED" -> "Livrable soumis";
            case "DELIVERABLE_APPROVED" -> "Livrable approuvé";
            case "DELIVERABLE_REJECTED" -> "Livrable rejeté";
            case "DELIVERABLE_DELETED" -> "Livrable supprimé";
            default -> action;
        };
    }

    private String actionIcon(String action) {
        return switch (action) {
            case "CREATED" -> "add";
            case "PROPOSED" -> "send";
            case "ACCEPTED" -> "check";
            case "REJECTED" -> "close";
            case "STARTED" -> "play";
            case "COMPLETED" -> "flag";
            case "CANCELLED" -> "ban";
            case "CLOSED" -> "lock";
            case "DELIVERABLE_ADDED" -> "file-plus";
            case "DELIVERABLE_SUBMITTED" -> "upload";
            case "DELIVERABLE_APPROVED" -> "check-circle";
            case "DELIVERABLE_REJECTED" -> "x-circle";
            default -> "info";
        };
    }

    private String actionColor(String action) {
        return switch (action) {
            case "CREATED", "DELIVERABLE_ADDED" -> "#007bff";
            case "PROPOSED", "DELIVERABLE_SUBMITTED" -> "#17a2b8";
            case "ACCEPTED", "COMPLETED", "DELIVERABLE_APPROVED" -> "#28a745";
            case "REJECTED", "CANCELLED", "DELIVERABLE_REJECTED" -> "#dc3545";
            case "CLOSED" -> "#6c757d";
            case "STARTED" -> "#ffc107";
            default -> "#495057";
        };
    }

    private String safeUserName(Long userId) {
        try {
            UserRemoteDto u = userClient.getUserById(userId);
            if (u != null) {
                String name = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                        + (u.getLastName() != null ? u.getLastName() : "")).trim();
                return name.isBlank() ? "User #" + userId : name;
            }
        } catch (Exception e) { /* fallback */ }
        return "User #" + userId;
    }
}
