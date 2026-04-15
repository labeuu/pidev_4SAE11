package com.esprit.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskScheduledJobs {

    private final TaskService taskService;

    @Value("${task.scheduler.purge-cancelled-days:90}")
    private int purgeCancelledDays;

    @Scheduled(cron = "${task.scheduler.escalate-cron:0 0 9 * * ?}")
    // Performs run escalate overdue priorities.
    public void runEscalateOverduePriorities() {
        int n = taskService.escalateOverduePriorities();
        if (n > 0) {
            log.info("Escalated {} overdue task(s) to HIGH priority", n);
        }
    }

    @Scheduled(cron = "${task.scheduler.purge-cron:0 0 4 ? * MON}")
    // Performs run purge old cancelled tasks.
    public void runPurgeOldCancelledTasks() {
        LocalDateTime cutoff = LocalDateTime.now().minus(purgeCancelledDays, ChronoUnit.DAYS);
        int n = taskService.purgeOldCancelledTasks(cutoff);
        if (n > 0) {
            log.info("Purged {} old CANCELLED task(s)", n);
        }
    }

    /** Daily in-app reminders to freelancers with overdue assigned tasks or subtasks. */
    @Scheduled(cron = "${task.scheduler.overdue-reminder-cron:0 0 8 * * ?}")
    // Performs run daily overdue reminders.
    public void runDailyOverdueReminders() {
        int n = taskService.sendDailyOverdueReminders();
        if (n > 0) {
            log.info("Sent daily overdue reminder(s) to {} freelancer(s)", n);
        }
    }
}
