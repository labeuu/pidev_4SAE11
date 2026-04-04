package com.esprit.task.service;

import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.ProjectActivityDto;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.SubtaskProgressDto;
import com.esprit.task.dto.TaskBoardDto;
import com.esprit.task.dto.TaskCalendarEventDto;
import com.esprit.task.dto.TaskStatsDto;
import com.esprit.task.entity.Subtask;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.SubtaskRepository;
import com.esprit.task.repository.TaskCommentRepository;
import com.esprit.task.repository.TaskRepository;
import com.esprit.task.repository.TaskSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final ProjectClient projectClient;
    private final TaskNotificationService taskNotificationService;

    @Transactional(readOnly = true)
    public Page<Task> findAllFiltered(Optional<Long> projectId, Optional<Long> contractId, Optional<Long> assigneeId,
                                      Optional<TaskStatus> status, Optional<TaskPriority> priority,
                                      Optional<String> search, Optional<LocalDate> dueDateFrom, Optional<LocalDate> dueDateTo,
                                      Pageable pageable) {
        var spec = TaskSpecification.filtered(projectId, contractId, assigneeId, status, priority, search, dueDateFrom, dueDateTo);
        return taskRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task", id));
    }

    @Transactional(readOnly = true)
    public List<Task> findByProjectId(Long projectId) {
        return taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<Task> findByContractId(Long contractId) {
        return taskRepository.findByContractIdOrderByProjectIdAscOrderIndexAsc(contractId);
    }

    @Transactional(readOnly = true)
    public List<Task> findByAssigneeId(Long assigneeId) {
        return taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(assigneeId);
    }

    /** Root tasks only (all rows in `task` are roots; subtasks are in `subtask`). */
    @Transactional(readOnly = true)
    public List<Task> findRootTasksByProject(Long projectId) {
        return taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<Task> findDueSoon(Optional<Long> projectId, Optional<Long> assigneeId, int withinDays) {
        int days = Math.min(Math.max(withinDays, 1), 365);
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        var spec = TaskSpecification.dueSoon(today, end, projectId, assigneeId);
        List<Task> tasks = taskRepository.findAll(spec,
                Sort.by(Sort.Order.asc("dueDate"), Sort.Order.asc("orderIndex"), Sort.Order.asc("projectId")));
        List<Subtask> subtasks;
        if (projectId.isPresent() && assigneeId.isPresent()) {
            subtasks = subtaskRepository.findDueSoonSubtasksByAssignee(today, end, assigneeId.get()).stream()
                    .filter(s -> s.getProjectId().equals(projectId.get()))
                    .toList();
        } else if (projectId.isPresent()) {
            subtasks = subtaskRepository.findDueSoonSubtasksByProject(today, end, projectId.get());
        } else if (assigneeId.isPresent()) {
            subtasks = subtaskRepository.findDueSoonSubtasksByAssignee(today, end, assigneeId.get());
        } else {
            subtasks = subtaskRepository.findDueSoonSubtasks(today, end);
        }
        List<Task> merged = new ArrayList<>(tasks);
        subtasks.stream().map(this::taskViewFromSubtask).forEach(merged::add);
        merged.sort(Comparator
                .comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())));
        return merged;
    }

    @Transactional(readOnly = true)
    public TaskBoardDto getBoardByProject(Long projectId) {
        List<Task> roots = taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
        Map<TaskStatus, List<Task>> columns = Arrays.stream(TaskStatus.values())
                .filter(s -> s != TaskStatus.CANCELLED)
                .collect(Collectors.toMap(s -> s, s -> new ArrayList<>()));
        for (Task t : roots) {
            if (t.getStatus() != TaskStatus.CANCELLED && columns.containsKey(t.getStatus())) {
                columns.get(t.getStatus()).add(t);
            }
        }
        return TaskBoardDto.builder()
                .projectId(projectId)
                .columns(columns)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Task> getOverdueTasks(Optional<Long> projectId, Optional<Long> assigneeId) {
        LocalDate today = LocalDate.now();
        List<Task> roots;
        List<Subtask> subs;
        if (projectId.isPresent() && assigneeId.isPresent()) {
            roots = taskRepository.findOverdueTasksByProject(projectId.get(), today).stream()
                    .filter(t -> t.getAssigneeId() != null && t.getAssigneeId().equals(assigneeId.get()))
                    .toList();
            subs = subtaskRepository.findOverdueSubtasksByAssignee(assigneeId.get(), today).stream()
                    .filter(s -> s.getProjectId().equals(projectId.get()))
                    .toList();
        } else if (projectId.isPresent()) {
            roots = taskRepository.findOverdueTasksByProject(projectId.get(), today);
            subs = subtaskRepository.findOverdueSubtasksByProject(projectId.get(), today);
        } else if (assigneeId.isPresent()) {
            roots = taskRepository.findOverdueTasksByAssignee(assigneeId.get(), today);
            subs = subtaskRepository.findOverdueSubtasksByAssignee(assigneeId.get(), today);
        } else {
            roots = taskRepository.findOverdueTasks(today);
            subs = subtaskRepository.findOverdueSubtasks(today);
        }
        List<Task> merged = new ArrayList<>(roots);
        subs.stream().map(this::taskViewFromSubtask).forEach(merged::add);
        return merged;
    }

    @Transactional(readOnly = true)
    public TaskStatsDto getStatsByProject(Long projectId) {
        long taskTotal = taskRepository.countByProjectId(projectId);
        long subTotal = subtaskRepository.countByProjectId(projectId);
        long total = taskTotal + subTotal;
        long done = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE)
                + subtaskRepository.countByProjectIdAndStatus(projectId, TaskStatus.DONE);
        long inProgress = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS)
                + taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_REVIEW)
                + subtaskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS)
                + subtaskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_REVIEW);
        long overdue = taskRepository.findOverdueTasksByProject(projectId, LocalDate.now()).size()
                + subtaskRepository.findOverdueSubtasksByProject(projectId, LocalDate.now()).size();
        double completionPercentage = total > 0 ? (100.0 * done / total) : 0.0;
        return TaskStatsDto.builder()
                .totalTasks(total)
                .doneCount(done)
                .inProgressCount(inProgress)
                .overdueCount(overdue)
                .completionPercentage(completionPercentage)
                .build();
    }

    @Transactional(readOnly = true)
    public TaskStatsDto getStatsByFreelancer(Long freelancerId, Optional<LocalDate> from, Optional<LocalDate> to) {
        var spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.of(freelancerId),
                Optional.empty(), Optional.empty(), Optional.empty(), from, to);
        List<Task> tasks = taskRepository.findAll(spec);
        List<Subtask> subtasks = subtaskRepository.findByAssigneeId(freelancerId).stream()
                .filter(s -> matchesDateRange(s.getDueDate(), from, to))
                .toList();
        long total = tasks.size() + subtasks.size();
        long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count()
                + subtasks.stream().filter(s -> s.getStatus() == TaskStatus.DONE).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.IN_REVIEW).count()
                + subtasks.stream().filter(s -> s.getStatus() == TaskStatus.IN_PROGRESS || s.getStatus() == TaskStatus.IN_REVIEW).count();
        LocalDate today = LocalDate.now();
        long overdue = tasks.stream().filter(t -> isOverdueOpen(t.getDueDate(), t.getStatus())).count()
                + subtasks.stream().filter(s -> isOverdueOpen(s.getDueDate(), s.getStatus())).count();
        double completionPercentage = total > 0 ? (100.0 * done / total) : 0.0;
        return TaskStatsDto.builder()
                .totalTasks(total)
                .doneCount(done)
                .inProgressCount(inProgress)
                .overdueCount(overdue)
                .completionPercentage(completionPercentage)
                .build();
    }

    private static boolean matchesDateRange(LocalDate due, Optional<LocalDate> from, Optional<LocalDate> to) {
        if (from.isEmpty() && to.isEmpty()) {
            return true;
        }
        if (due == null) {
            return false;
        }
        if (from.isPresent() && due.isBefore(from.get())) {
            return false;
        }
        return to.isEmpty() || !due.isAfter(to.get());
    }

    private static boolean isOverdueOpen(LocalDate due, TaskStatus status) {
        if (due == null || status == TaskStatus.DONE || status == TaskStatus.CANCELLED) {
            return false;
        }
        return due.isBefore(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public TaskStatsDto getDashboardStats() {
        List<Task> allTasks = taskRepository.findAll();
        long subCount = subtaskRepository.count();
        long total = allTasks.size() + subCount;
        long doneCount = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count()
                + subtaskRepository.findAll().stream().filter(s -> s.getStatus() == TaskStatus.DONE).count();
        long inProgressCount = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.IN_REVIEW).count()
                + subtaskRepository.findAll().stream().filter(s -> s.getStatus() == TaskStatus.IN_PROGRESS || s.getStatus() == TaskStatus.IN_REVIEW).count();
        long overdueCount = taskRepository.findOverdueTasks(LocalDate.now()).size()
                + subtaskRepository.findOverdueSubtasks(LocalDate.now()).size();
        return TaskStatsDto.builder()
                .totalTasks(total)
                .doneCount(doneCount)
                .inProgressCount(inProgressCount)
                .overdueCount(overdueCount)
                .completionPercentage(total > 0 ? (100.0 * doneCount / total) : 0.0)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TaskCalendarEventDto> getCalendarEvents(LocalDateTime timeMin, LocalDateTime timeMax, Optional<Long> userId) {
        LocalDate start = timeMin.toLocalDate();
        LocalDate end = timeMax.toLocalDate();
        List<Task> tasks;
        if (userId.isPresent()) {
            Set<Long> projectIds = new HashSet<>();
            try {
                List<ProjectDto> clientProjects = projectClient.getProjectsByClientId(userId.get());
                if (clientProjects != null) {
                    clientProjects.stream().map(ProjectDto::getId).filter(Objects::nonNull).forEach(projectIds::add);
                }
            } catch (Exception e) {
                log.warn("Failed to load client projects for calendar filter: {}", e.getMessage());
            }
            List<Task> byAssignee = taskRepository.findByDueDateBetweenAndAssigneeId(start, end, userId.get());
            Set<Long> assigneeTaskIds = byAssignee.stream().map(Task::getId).collect(Collectors.toSet());
            List<Task> byProject = taskRepository.findByDueDateBetween(start, end).stream()
                    .filter(t -> projectIds.contains(t.getProjectId()) && !assigneeTaskIds.contains(t.getId()))
                    .toList();
            tasks = new ArrayList<>(byAssignee);
            tasks.addAll(byProject);
        } else {
            tasks = taskRepository.findByDueDateBetween(start, end);
        }
        List<TaskCalendarEventDto> taskEvents = tasks.stream()
                .map(t -> TaskCalendarEventDto.builder()
                        .id("task-" + t.getId())
                        .summary("Task deadline – " + (t.getTitle() != null ? t.getTitle() : "Task #" + t.getId()))
                        .start(t.getDueDate().atStartOfDay())
                        .end(t.getDueDate().atStartOfDay().plus(1, ChronoUnit.HOURS))
                        .description(t.getDescription())
                        .build())
                .toList();

        List<Subtask> subtasksInRange = subtaskRepository.findByDueDateBetween(start, end);
        List<Subtask> filteredSubs;
        if (userId.isPresent()) {
            Set<Long> projectIds = new HashSet<>();
            try {
                List<ProjectDto> clientProjects = projectClient.getProjectsByClientId(userId.get());
                if (clientProjects != null) {
                    clientProjects.stream().map(ProjectDto::getId).filter(Objects::nonNull).forEach(projectIds::add);
                }
            } catch (Exception e) {
                log.warn("Failed to load client projects for subtask calendar filter: {}", e.getMessage());
            }
            List<Subtask> byAssigneeSub = subtaskRepository.findByDueDateBetweenAndAssigneeId(start, end, userId.get());
            Set<Long> seen = byAssigneeSub.stream().map(Subtask::getId).collect(Collectors.toSet());
            List<Subtask> byProjectSub = subtasksInRange.stream()
                    .filter(s -> projectIds.contains(s.getProjectId()) && !seen.contains(s.getId()))
                    .toList();
            filteredSubs = Stream.concat(byAssigneeSub.stream(), byProjectSub.stream()).toList();
        } else {
            filteredSubs = subtasksInRange;
        }
        List<TaskCalendarEventDto> subEvents = filteredSubs.stream()
                .map(s -> TaskCalendarEventDto.builder()
                        .id("subtask-" + s.getId())
                        .summary("Subtask – " + (s.getTitle() != null ? s.getTitle() : "Subtask #" + s.getId()))
                        .start(s.getDueDate().atStartOfDay())
                        .end(s.getDueDate().atStartOfDay().plus(1, ChronoUnit.HOURS))
                        .description(s.getDescription())
                        .build())
                .toList();

        List<TaskCalendarEventDto> merged = new ArrayList<>(taskEvents);
        merged.addAll(subEvents);
        return merged;
    }

    @Transactional
    public Task create(Task task) {
        if (task.getStatus() == null) task.setStatus(TaskStatus.TODO);
        if (task.getPriority() == null) task.setPriority(TaskPriority.MEDIUM);
        if (task.getOrderIndex() == null) {
            Integer max = taskRepository.findMaxOrderIndexByProject(task.getProjectId());
            task.setOrderIndex(max != null ? max + 1 : 0);
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, Task task) {
        Task existing = findById(id);
        TaskStatus oldStatus = existing.getStatus();
        existing.setProjectId(task.getProjectId());
        existing.setContractId(task.getContractId());
        existing.setTitle(task.getTitle());
        existing.setDescription(task.getDescription());
        existing.setStatus(task.getStatus() != null ? task.getStatus() : existing.getStatus());
        existing.setPriority(task.getPriority() != null ? task.getPriority() : existing.getPriority());
        existing.setAssigneeId(task.getAssigneeId());
        existing.setDueDate(task.getDueDate());
        if (task.getOrderIndex() != null) existing.setOrderIndex(task.getOrderIndex());
        Task saved = taskRepository.save(existing);
        if (task.getStatus() != null && !task.getStatus().equals(oldStatus)) {
            taskNotificationService.notifyTaskStatusUpdate(saved);
        }
        return saved;
    }

    @Transactional
    public Task patchStatus(Long id, TaskStatus status) {
        Task task = findById(id);
        task.setStatus(status);
        Task saved = taskRepository.save(task);
        taskNotificationService.notifyTaskStatusUpdate(saved);
        return saved;
    }

    @Transactional
    public Task patchAssignee(Long id, Long assigneeId) {
        Task task = findById(id);
        task.setAssigneeId(assigneeId);
        return taskRepository.save(task);
    }

    @Transactional
    public Task patchDueDate(Long id, LocalDate dueDate) {
        Task task = findById(id);
        task.setDueDate(dueDate);
        return taskRepository.save(task);
    }

    @Transactional
    public List<Task> bulkPatchStatus(List<Long> taskIds, TaskStatus newStatus) {
        List<Task> result = new ArrayList<>();
        for (Long id : taskIds) {
            Task t = findById(id);
            TaskStatus old = t.getStatus();
            t.setStatus(newStatus);
            Task saved = taskRepository.save(t);
            if (newStatus != null && !newStatus.equals(old)) {
                taskNotificationService.notifyTaskStatusUpdate(saved);
            }
            result.add(saved);
        }
        return result;
    }

    @Transactional
    public void reorder(List<Long> taskIds) {
        if (taskIds == null) {
            throw new IllegalArgumentException("taskIds is required");
        }
        for (int i = 0; i < taskIds.size(); i++) {
            final int orderIndex = i;
            taskRepository.findById(taskIds.get(i)).ifPresent(t -> {
                t.setOrderIndex(orderIndex);
                taskRepository.save(t);
            });
        }
    }

    @Transactional
    public void deleteById(Long id) {
        Task task = findById(id);
        taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(id).forEach(taskCommentRepository::delete);
        taskRepository.delete(task);
    }

    @Transactional
    public int escalateOverduePriorities() {
        LocalDate today = LocalDate.now();
        int escalated = 0;
        List<Task> overdueTasks = taskRepository.findOverdueTasks(today);
        for (Task t : overdueTasks) {
            if (t.getPriority() == TaskPriority.LOW || t.getPriority() == TaskPriority.MEDIUM) {
                t.setPriority(TaskPriority.HIGH);
                taskRepository.save(t);
                escalated++;
                if (t.getAssigneeId() != null) {
                    taskNotificationService.notifyTaskPriorityEscalated(t);
                }
            }
        }
        List<Subtask> overdueSubs = subtaskRepository.findOverdueSubtasks(today);
        for (Subtask s : overdueSubs) {
            if (s.getPriority() == TaskPriority.LOW || s.getPriority() == TaskPriority.MEDIUM) {
                s.setPriority(TaskPriority.HIGH);
                subtaskRepository.save(s);
                escalated++;
            }
        }
        return escalated;
    }

    @Transactional
    public int purgeOldCancelledTasks(LocalDateTime cutoff) {
        List<Task> candidates = taskRepository.findByStatusAndUpdatedAtBefore(TaskStatus.CANCELLED, cutoff);
        int removed = 0;
        for (Task t : candidates) {
            if (!taskRepository.existsById(t.getId())) {
                continue;
            }
            deleteById(t.getId());
            removed++;
        }
        return removed;
    }

    private static final int MAX_SUBTASK_PROGRESS_TASK_IDS = 100;

    @Transactional(readOnly = true)
    public List<SubtaskProgressDto> getSubtaskProgressForAssigneeTasks(Long assigneeId, List<Long> taskIds) {
        if (assigneeId == null || taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = taskIds.stream().filter(Objects::nonNull).distinct().limit(MAX_SUBTASK_PROGRESS_TASK_IDS).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return subtaskRepository.aggregateSubtaskProgressByParent(assigneeId, ids).stream()
                .map(r -> SubtaskProgressDto.builder()
                        .parentTaskId((Long) r[0])
                        .total(((Number) r[1]).longValue())
                        .completed(((Number) r[2]).longValue())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectActivityDto> getProjectActivityForAssignee(Long assigneeId) {
        if (assigneeId == null) {
            return List.of();
        }
        Map<Long, LocalDateTime> taskMaxByProject = new HashMap<>();
        for (Object[] row : taskRepository.findMaxTaskUpdatedByProjectForAssignee(assigneeId)) {
            taskMaxByProject.put((Long) row[0], (LocalDateTime) row[1]);
        }
        Map<Long, LocalDateTime> subMaxByProject = new HashMap<>();
        for (Object[] row : subtaskRepository.findMaxSubtaskUpdatedByProjectForAssignee(assigneeId)) {
            subMaxByProject.put((Long) row[0], (LocalDateTime) row[1]);
        }
        Map<Long, Long> openByProject = new HashMap<>();
        for (Object[] row : taskRepository.countOpenTasksByProjectForAssignee(assigneeId)) {
            openByProject.put((Long) row[0], ((Number) row[1]).longValue());
        }
        Set<Long> projectIds = new HashSet<>();
        projectIds.addAll(taskMaxByProject.keySet());
        projectIds.addAll(subMaxByProject.keySet());
        projectIds.addAll(openByProject.keySet());
        List<ProjectActivityDto> out = new ArrayList<>();
        for (Long projectId : projectIds) {
            LocalDateTime tTask = taskMaxByProject.get(projectId);
            LocalDateTime tSub = subMaxByProject.get(projectId);
            LocalDateTime last;
            if (tTask == null) {
                last = tSub;
            } else if (tSub == null) {
                last = tTask;
            } else {
                last = tTask.isAfter(tSub) ? tTask : tSub;
            }
            out.add(ProjectActivityDto.builder()
                    .projectId(projectId)
                    .lastActivityAt(last)
                    .openTaskCount(openByProject.getOrDefault(projectId, 0L))
                    .build());
        }
        out.sort(Comparator
                .comparing(ProjectActivityDto::getLastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProjectActivityDto::getOpenTaskCount, Comparator.reverseOrder())
                .thenComparing(ProjectActivityDto::getProjectId, Comparator.reverseOrder()));
        return out;
    }

    private Task taskViewFromSubtask(Subtask s) {
        Task t = new Task();
        t.setSubtask(true);
        t.setId(s.getId());
        t.setProjectId(s.getProjectId());
        t.setContractId(null);
        t.setTitle(s.getTitle());
        t.setDescription(s.getDescription());
        t.setStatus(s.getStatus());
        t.setPriority(s.getPriority());
        t.setAssigneeId(s.getAssigneeId());
        t.setDueDate(s.getDueDate());
        t.setOrderIndex(s.getOrderIndex());
        t.setCreatedAt(s.getCreatedAt());
        t.setUpdatedAt(s.getUpdatedAt());
        return t;
    }
}
