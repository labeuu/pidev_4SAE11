package com.esprit.task.service;

import com.esprit.task.dto.SubtaskRequest;
import com.esprit.task.dto.SubtaskResponse;
import com.esprit.task.entity.Subtask;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.SubtaskRepository;
import com.esprit.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubtaskService {

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;
    private final TaskNotificationService taskNotificationService;

    @Transactional(readOnly = true)
    // Lists by parent task id.
    public List<SubtaskResponse> listByParentTaskId(Long parentTaskId) {
        taskRepository.findById(parentTaskId)
                .orElseThrow(() -> new EntityNotFoundException("Task", parentTaskId));
        return subtaskRepository.findByParent_IdOrderByOrderIndexAsc(parentTaskId).stream()
                .map(SubtaskResponse::from)
                .toList();
    }

    @Transactional
    // Creates this operation.
    public SubtaskResponse create(Long parentTaskId, SubtaskRequest request) {
        Task parent = taskRepository.findById(parentTaskId)
                .orElseThrow(() -> new EntityNotFoundException("Task", parentTaskId));
        Subtask s = Subtask.builder()
                .parent(parent)
                .projectId(parent.getProjectId())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .assigneeId(request.getAssigneeId() != null ? request.getAssigneeId() : parent.getAssigneeId())
                .dueDate(request.getDueDate())
                .orderIndex(resolveOrderIndex(parentTaskId, request.getOrderIndex()))
                .build();
        Subtask saved = subtaskRepository.save(s);
        return SubtaskResponse.from(saved);
    }

    // Performs resolve order index.
    private int resolveOrderIndex(Long parentTaskId, Integer requested) {
        if (requested != null) {
            return requested;
        }
        Integer max = subtaskRepository.findMaxOrderIndexByParent(parentTaskId);
        return max != null ? max + 1 : 0;
    }

    @Transactional(readOnly = true)
    // Finds entity by id.
    public Subtask findEntityById(Long id) {
        return subtaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subtask", id));
    }

    @Transactional
    // Updates this operation.
    public SubtaskResponse update(Long id, SubtaskRequest request) {
        Subtask existing = findEntityById(id);
        TaskStatus oldStatus = existing.getStatus();
        existing.setTitle(request.getTitle() != null ? request.getTitle().trim() : existing.getTitle());
        existing.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            existing.setAssigneeId(request.getAssigneeId());
        }
        existing.setDueDate(request.getDueDate());
        if (request.getOrderIndex() != null) {
            existing.setOrderIndex(request.getOrderIndex());
        }
        Subtask saved = subtaskRepository.save(existing);
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            taskNotificationService.notifySubtaskStatusUpdate(saved);
        }
        return SubtaskResponse.from(saved);
    }

    @Transactional
    // Partially updates status.
    public SubtaskResponse patchStatus(Long id, TaskStatus status) {
        Subtask s = findEntityById(id);
        TaskStatus old = s.getStatus();
        s.setStatus(status);
        Subtask saved = subtaskRepository.save(s);
        if (status != null && !status.equals(old)) {
            taskNotificationService.notifySubtaskStatusUpdate(saved);
        }
        return SubtaskResponse.from(saved);
    }

    @Transactional
    // Partially updates due date.
    public SubtaskResponse patchDueDate(Long id, LocalDate dueDate) {
        Subtask s = findEntityById(id);
        s.setDueDate(dueDate);
        return SubtaskResponse.from(subtaskRepository.save(s));
    }

    @Transactional
    // Deletes by id.
    public void deleteById(Long id) {
        Subtask s = subtaskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subtask", id));
        subtaskRepository.delete(s);
    }
}
