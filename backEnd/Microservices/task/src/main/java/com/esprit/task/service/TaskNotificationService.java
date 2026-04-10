package com.esprit.task.service;

import com.esprit.task.client.NotificationClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.NotificationRequestDto;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.entity.Subtask;
import com.esprit.task.entity.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends notifications to the Notification microservice when a task's status changes.
 * Failures are logged but do not affect the calling flow (fire-and-forget).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskNotificationService {

    private final NotificationClient notificationClient;
    private final ProjectClient projectClient;

    public static final String TYPE_TASK_STATUS_UPDATE = "TASK_STATUS_UPDATE";
    public static final String TYPE_TASK_PRIORITY_ESCALATED = "TASK_PRIORITY_ESCALATED";
    /** Daily cron: reminder to freelancers with any overdue assigned work (tasks + subtasks). */
    public static final String TYPE_TASK_OVERDUE_DAILY_REMINDER = "TASK_OVERDUE_DAILY_REMINDER";

    private static final int MAX_OVERDUE_LINES_IN_BODY = 12;

    /**
     * Notify the project client when a task's status was updated.
     * Fetches the project to obtain clientId, then sends a notification.
     */
    public void notifyTaskStatusUpdate(Task task) {
        if (task == null || task.getProjectId() == null) {
            return;
        }
        ProjectDto project;
        try {
            project = projectClient.getProjectById(task.getProjectId());
        } catch (Exception e) {
            log.warn("Failed to load project {} for task status notification: {}", task.getProjectId(), e.getMessage());
            return;
        }
        if (project == null || project.getClientId() == null) {
            return;
        }
        String userId = String.valueOf(project.getClientId());
        String taskTitle = task.getTitle() != null ? task.getTitle() : "Task #" + task.getId();
        String statusLabel = task.getStatus() != null ? task.getStatus().name().replace("_", " ") : "updated";
        String title = "Task status updated";
        String body = String.format("Task \"%s\" is now %s.", taskTitle, statusLabel);

        Map<String, String> data = new HashMap<>();
        data.put("projectId", String.valueOf(task.getProjectId()));
        data.put("taskId", String.valueOf(task.getId()));

        notifyUser(userId, title, body, TYPE_TASK_STATUS_UPDATE, data);
    }

    /**
     * Notify the project client when a subtask status was updated.
     */
    public void notifySubtaskStatusUpdate(Subtask subtask) {
        if (subtask == null || subtask.getProjectId() == null) {
            return;
        }
        ProjectDto project;
        try {
            project = projectClient.getProjectById(subtask.getProjectId());
        } catch (Exception e) {
            log.warn("Failed to load project {} for subtask status notification: {}", subtask.getProjectId(), e.getMessage());
            return;
        }
        if (project == null || project.getClientId() == null) {
            return;
        }
        String userId = String.valueOf(project.getClientId());
        String subTitle = subtask.getTitle() != null ? subtask.getTitle() : "Subtask #" + subtask.getId();
        String statusLabel = subtask.getStatus() != null ? subtask.getStatus().name().replace("_", " ") : "updated";
        String title = "Subtask status updated";
        String body = String.format("Subtask \"%s\" is now %s.", subTitle, statusLabel);

        Map<String, String> data = new HashMap<>();
        data.put("projectId", String.valueOf(subtask.getProjectId()));
        if (subtask.getParent() != null) {
            data.put("taskId", String.valueOf(subtask.getParent().getId()));
        }
        data.put("subtaskId", String.valueOf(subtask.getId()));

        notifyUser(userId, title, body, TYPE_TASK_STATUS_UPDATE, data);
    }

    /** Overdue LOW/MEDIUM → HIGH: default notification copy. */
    public void notifyTaskPriorityEscalated(Task task) {
        notifyTaskPriorityEscalated(task, "HIGH");
    }

    /**
     * Notify the assignee when a scheduled policy escalates task priority (overdue, stuck IN_REVIEW, etc.).
     *
     * @param newPriorityLabel human-readable target priority (e.g. HIGH, URGENT)
     */
    public void notifyTaskPriorityEscalated(Task task, String newPriorityLabel) {
        if (task == null || task.getAssigneeId() == null) {
            return;
        }
        String userId = String.valueOf(task.getAssigneeId());
        String taskTitle = task.getTitle() != null ? task.getTitle() : "Task #" + task.getId();
        String title = "Task priority escalated";
        String label = newPriorityLabel != null ? newPriorityLabel : "HIGH";
        String body = String.format(
                "Task \"%s\" was escalated to %s priority by automated policy. Please update or complete it.",
                taskTitle, label);
        Map<String, String> data = new HashMap<>();
        data.put("projectId", String.valueOf(task.getProjectId()));
        data.put("taskId", String.valueOf(task.getId()));
        notifyUser(userId, title, body, TYPE_TASK_PRIORITY_ESCALATED, data);
    }

    /**
     * Notify assignee when a subtask priority is raised by the escalation scheduler.
     */
    public void notifySubtaskPriorityEscalated(Subtask subtask, String newPriorityLabel) {
        if (subtask == null || subtask.getAssigneeId() == null) {
            return;
        }
        String userId = String.valueOf(subtask.getAssigneeId());
        String subTitle = subtask.getTitle() != null ? subtask.getTitle() : "Subtask #" + subtask.getId();
        String label = newPriorityLabel != null ? newPriorityLabel : "HIGH";
        String title = "Subtask priority escalated";
        String body = String.format(
                "Subtask \"%s\" was escalated to %s priority by automated policy.",
                subTitle, label);
        Map<String, String> data = new HashMap<>();
        data.put("projectId", String.valueOf(subtask.getProjectId()));
        data.put("subtaskId", String.valueOf(subtask.getId()));
        notifyUser(userId, title, body, TYPE_TASK_PRIORITY_ESCALATED, data);
    }

    /**
     * Optional: inform project client that work was auto-escalated (gated by {@code task.escalation.notify-client-on-escalation}).
     */
    public void notifyClientTaskPriorityEscalated(Task task, String newPriorityLabel) {
        if (task == null || task.getProjectId() == null) {
            return;
        }
        ProjectDto project;
        try {
            project = projectClient.getProjectById(task.getProjectId());
        } catch (Exception e) {
            log.warn("Failed to load project {} for client escalation notification: {}", task.getProjectId(), e.getMessage());
            return;
        }
        if (project == null || project.getClientId() == null) {
            return;
        }
        String userId = String.valueOf(project.getClientId());
        String taskTitle = task.getTitle() != null ? task.getTitle() : "Task #" + task.getId();
        String label = newPriorityLabel != null ? newPriorityLabel : "HIGH";
        String title = "Task priority auto-escalated";
        String body = String.format(
                "\"%s\" was escalated to %s priority due to schedule/risk policy. The assignee was notified.",
                taskTitle, label);
        Map<String, String> data = new HashMap<>();
        data.put("projectId", String.valueOf(task.getProjectId()));
        data.put("taskId", String.valueOf(task.getId()));
        notifyUser(userId, title, body, TYPE_TASK_PRIORITY_ESCALATED, data);
    }

    /**
     * Daily digest for one freelancer: lists overdue tasks/subtasks assigned to them.
     * Fire-and-forget; failures are logged only.
     */
    public void notifyFreelancerDailyOverdueReminder(Long assigneeId, List<String> itemLines) {
        if (assigneeId == null || itemLines == null || itemLines.isEmpty()) {
            return;
        }
        String userId = String.valueOf(assigneeId);
        int n = itemLines.size();
        String title = "Overdue work — daily reminder";
        StringBuilder body = new StringBuilder();
        body.append("You have ").append(n).append(" overdue item(s). Please prioritize them in My Tasks as soon as possible.\n\n");
        int show = Math.min(n, MAX_OVERDUE_LINES_IN_BODY);
        for (int i = 0; i < show; i++) {
            body.append("• ").append(itemLines.get(i)).append('\n');
        }
        if (n > show) {
            body.append("… and ").append(n - show).append(" more (see My Tasks → Overdue).\n");
        }
        Map<String, String> data = new HashMap<>();
        data.put("overdueCount", String.valueOf(n));
        data.put("reminderKind", "OVERDUE_DAILY");
        notifyUser(userId, title, body.toString().trim(), TYPE_TASK_OVERDUE_DAILY_REMINDER, data);
    }

    // Performs notify user.
    private void notifyUser(String userId, String title, String body, String type, Map<String, String> data) {
        if (userId == null || title == null) {
            return;
        }
        try {
            NotificationRequestDto request = NotificationRequestDto.builder()
                    .userId(userId)
                    .title(title)
                    .body(body != null ? body : "")
                    .type(type != null ? type : "GENERAL")
                    .data(data)
                    .build();
            notificationClient.create(request);
        } catch (Exception e) {
            log.warn("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }
}
