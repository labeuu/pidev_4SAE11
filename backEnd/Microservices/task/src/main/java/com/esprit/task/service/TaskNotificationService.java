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

    /**
     * Notify the assignee when a scheduled job escalates an overdue task to HIGH priority.
     */
    public void notifyTaskPriorityEscalated(Task task) {
        if (task == null || task.getAssigneeId() == null) {
            return;
        }
        String userId = String.valueOf(task.getAssigneeId());
        String taskTitle = task.getTitle() != null ? task.getTitle() : "Task #" + task.getId();
        String title = "Task priority escalated";
        String body = String.format(
                "Overdue task \"%s\" was escalated to HIGH priority. Please update or complete it.",
                taskTitle);
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
