package org.example.subcontracting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.dto.NotificationRequestDto;
import org.example.subcontracting.entity.DeliverableStatus;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractDeliverable;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.example.subcontracting.entity.SubcontractStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MÉTIER 1 — Notifications proactives + MÉTIER 2 — Expiration automatique deadlines.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubcontractNotificationService {

    private final SubcontractRepository subcontractRepo;
    private final SubcontractDeliverableRepository deliverableRepo;
    private final NotificationFeignClient notificationClient;

    /**
     * MÉTIER 2 — Cron : détecte les livrables en retard et notifie.
     * Exécuté tous les jours à 8h.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public int checkOverdueDeliverables() {
        LocalDate today = LocalDate.now();
        List<Subcontract> active = subcontractRepo.findByStatusOrderByCreatedAtDesc(SubcontractStatus.IN_PROGRESS);
        int count = 0;

        for (Subcontract sc : active) {
            List<SubcontractDeliverable> deliverables = deliverableRepo
                    .findBySubcontractIdOrderByDeadlineAsc(sc.getId());
            for (SubcontractDeliverable d : deliverables) {
                if (d.getDeadline() != null && today.isAfter(d.getDeadline())
                        && d.getStatus() != DeliverableStatus.APPROVED) {
                    notify(sc.getSubcontractorId(), "DELIVERABLE_OVERDUE",
                            "Livrable en retard",
                            "Le livrable '" + d.getTitle() + "' de la sous-traitance '" + sc.getTitle()
                                    + "' a dépassé sa deadline (" + d.getDeadline() + ").");
                    notify(sc.getMainFreelancerId(), "DELIVERABLE_OVERDUE",
                            "Livrable en retard",
                            "Le livrable '" + d.getTitle() + "' du sous-traitant est en retard.");
                    count++;
                }
            }

            if (sc.getDeadline() != null && today.isAfter(sc.getDeadline())) {
                notify(sc.getMainFreelancerId(), "SUBCONTRACT_OVERDUE",
                        "Sous-traitance en retard",
                        "La sous-traitance '" + sc.getTitle() + "' a dépassé sa deadline globale.");
                count++;
            }
        }

        log.info("[SUBCONTRACT-CRON] Checked overdue: {} alerts sent", count);
        return count;
    }

    /**
     * MÉTIER 2 — Cron : rappel 3 jours avant deadline.
     * Exécuté tous les jours à 9h.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public int sendDeadlineReminders() {
        LocalDate in3Days = LocalDate.now().plusDays(3);
        LocalDate today = LocalDate.now();
        List<Subcontract> active = subcontractRepo.findByStatusOrderByCreatedAtDesc(SubcontractStatus.IN_PROGRESS);
        int count = 0;

        for (Subcontract sc : active) {
            List<SubcontractDeliverable> deliverables = deliverableRepo
                    .findBySubcontractIdOrderByDeadlineAsc(sc.getId());
            for (SubcontractDeliverable d : deliverables) {
                if (d.getDeadline() != null
                        && !today.isAfter(d.getDeadline())
                        && !in3Days.isBefore(d.getDeadline())
                        && d.getStatus() != DeliverableStatus.APPROVED
                        && d.getStatus() != DeliverableStatus.SUBMITTED) {
                    notify(sc.getSubcontractorId(), "DELIVERABLE_DEADLINE_SOON",
                            "Deadline dans 3 jours",
                            "Le livrable '" + d.getTitle() + "' doit être soumis avant le " + d.getDeadline() + ".");
                    count++;
                }
            }
        }

        log.info("[SUBCONTRACT-CRON] Deadline reminders: {} sent", count);
        return count;
    }

    private void notify(Long userId, String type, String title, String message) {
        try {
            notificationClient.sendNotification(NotificationRequestDto.builder()
                    .userId(userId).type(type).title(title).message(message).build());
        } catch (Exception e) {
            log.warn("[SUBCONTRACT] Notification failed for user={}: {}", userId, e.getMessage());
        }
    }
}
