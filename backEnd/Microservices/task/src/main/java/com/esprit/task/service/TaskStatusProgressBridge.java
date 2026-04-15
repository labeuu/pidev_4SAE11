package com.esprit.task.service;

import com.esprit.task.client.PlanningClient;
import com.esprit.task.dto.planning.PlanningProgressUpdateCreateDto;
import com.esprit.task.dto.planning.PlanningProgressUpdateDto;
import com.esprit.task.entity.Subtask;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskStatus;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * When task-board status changes, creates a corresponding Planning progress update (non-blocking).
 * Respects Planning's non-decreasing progress rule per project.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskStatusProgressBridge {

    private final PlanningClient planningClient;

    @Value("${task.planning.auto-progress-on-status-change:true}")
    private boolean autoProgressOnStatusChange;

    /**
     * Call after a root {@link Task} status change was persisted and notifications may run.
     */
    public void afterRootTaskStatusChanged(Task task) {
        if (!autoProgressOnStatusChange || task == null || task.getAssigneeId() == null || task.getProjectId() == null) {
            return;
        }
        TaskStatus status = task.getStatus();
        if (status == null) {
            return;
        }
        String label = task.getTitle() != null && !task.getTitle().isBlank() ? task.getTitle() : "Task #" + task.getId();
        String title = "Task board: \"" + label + "\" → " + status.name();
        String description = "Automatic update from task board (root task id " + task.getId() + ").";
        submit(task.getProjectId(), task.getContractId(), task.getAssigneeId(), title, description, status);
    }

    /**
     * Call after a {@link Subtask} status change was persisted.
     */
    public void afterSubtaskStatusChanged(Subtask subtask) {
        if (!autoProgressOnStatusChange || subtask == null || subtask.getAssigneeId() == null || subtask.getProjectId() == null) {
            return;
        }
        TaskStatus status = subtask.getStatus();
        if (status == null) {
            return;
        }
        Long contractId = null;
        if (subtask.getParent() != null) {
            contractId = subtask.getParent().getContractId();
        }
        String label = subtask.getTitle() != null && !subtask.getTitle().isBlank()
                ? subtask.getTitle()
                : "Subtask #" + subtask.getId();
        String title = "Task board (subtask): \"" + label + "\" → " + status.name();
        String description = "Automatic update from task board (subtask id " + subtask.getId() + ").";
        submit(subtask.getProjectId(), contractId, subtask.getAssigneeId(), title, description, status);
    }

    private void submit(
            Long projectId,
            Long contractId,
            Long freelancerId,
            String title,
            String description,
            TaskStatus status) {
        int minAllowed;
        try {
            List<PlanningProgressUpdateDto> updates = planningClient.listProgressUpdatesByProject(projectId);
            minAllowed = updates.stream()
                    .map(PlanningProgressUpdateDto::getProgressPercentage)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
        } catch (FeignException e) {
            log.warn("Planning progress list failed for project {}; skipping auto progress update: {}", projectId, e.getMessage());
            return;
        } catch (Exception e) {
            log.warn("Planning progress list failed for project {}; skipping auto progress update: {}", projectId, e.getMessage());
            return;
        }
        int target = resolveProgressPercentage(minAllowed, status);
        if (target < minAllowed) {
            target = minAllowed;
        }
        if (target > 100) {
            target = 100;
        }
        PlanningProgressUpdateCreateDto body = PlanningProgressUpdateCreateDto.builder()
                .projectId(projectId)
                .contractId(contractId)
                .freelancerId(freelancerId)
                .title(title)
                .description(description)
                .progressPercentage(target)
                .build();
        try {
            planningClient.createProgressUpdate(body);
        } catch (FeignException e) {
            log.warn("Planning create progress update failed for project {}: {}", projectId, e.getMessage());
        } catch (Exception e) {
            log.warn("Planning create progress update failed for project {}: {}", projectId, e.getMessage());
        }
    }

    static int resolveProgressPercentage(int minAllowed, TaskStatus status) {
        if (status == null) {
            return minAllowed;
        }
        return switch (status) {
            case TODO -> 25;
            case IN_PROGRESS -> 55;
            case IN_REVIEW -> 85;
            case DONE -> 100;
            case CANCELLED -> minAllowed;
        };
    }
}
