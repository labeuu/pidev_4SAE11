package com.esprit.planning.service;

import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressUpdateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled maintenance: overdue next-update-due notifications and orphan calendar event id cleanup.
 */
@Component
public class PlanningScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(PlanningScheduledJobs.class);

    private final ProgressUpdateRepository progressUpdateRepository;
    private final PlanningNotificationService planningNotificationService;

    public PlanningScheduledJobs(ProgressUpdateRepository progressUpdateRepository,
                                 PlanningNotificationService planningNotificationService) {
        this.progressUpdateRepository = progressUpdateRepository;
        this.planningNotificationService = planningNotificationService;
    }

    @Scheduled(cron = "${planning.scheduler.overdue-cron:0 0 * * * ?}")
    @Transactional
    // Performs notify overdue next progress due.
    public void notifyOverdueNextProgressDue() {
        LocalDateTime now = LocalDateTime.now();
        List<ProgressUpdate> overdue = progressUpdateRepository
                .findByNextUpdateDueIsNotNullAndNextUpdateDueBeforeAndNextDueOverdueNotifiedIsFalse(now);
        for (ProgressUpdate p : overdue) {
            p.setNextDueOverdueNotified(true);
            progressUpdateRepository.save(p);
            Map<String, String> data = new HashMap<>();
            data.put("projectId", String.valueOf(p.getProjectId()));
            data.put("progressUpdateId", String.valueOf(p.getId()));
            planningNotificationService.notifyUser(
                    String.valueOf(p.getFreelancerId()),
                    "Progress update due date passed",
                    "Your scheduled next progress update (\"" + (p.getTitle() != null ? p.getTitle() : "#" + p.getId())
                            + "\") is overdue. Please submit an update.",
                    PlanningNotificationService.TYPE_PROGRESS_NEXT_DUE_OVERDUE,
                    data);
        }
        if (!overdue.isEmpty()) {
            log.info("Marked {} progress update(s) as overdue-notified", overdue.size());
        }
    }

    @Scheduled(cron = "${planning.scheduler.cleanup-cron:0 0 3 ? * SUN}")
    @Transactional
    // Performs clear orphan next due calendar event ids.
    public void clearOrphanNextDueCalendarEventIds() {
        int cleared = progressUpdateRepository.clearOrphanNextDueCalendarEventIds();
        if (cleared > 0) {
            log.info("Cleared {} orphan nextDueCalendarEventId value(s)", cleared);
        }
    }
}
