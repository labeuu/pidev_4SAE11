package com.esprit.task.service;

import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.ProjectActivityDto;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.SubtaskProgressDto;
import com.esprit.task.dto.TaskBoardDto;
import com.esprit.task.dto.TaskCalendarEventDto;
import com.esprit.task.dto.TaskPriorityCountDto;
import com.esprit.task.dto.TaskStatsDto;
import com.esprit.task.dto.TaskStatsExtendedDto;
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
                                      Optional<Boolean> openTasksOnly,
                                      Pageable pageable) {
        var spec = TaskSpecification.filtered(projectId, contractId, assigneeId, status, priority, search, dueDateFrom, dueDateTo, openTasksOnly);
        return taskRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    // Finds by id.
    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task", id));
    }

    @Transactional(readOnly = true)
    // Finds by project id.
    public List<Task> findByProjectId(Long projectId) {
        return taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
    }

    @Transactional(readOnly = true)
    // Finds by contract id.
    public List<Task> findByContractId(Long contractId) {
        return taskRepository.findByContractIdOrderByProjectIdAscOrderIndexAsc(contractId);
    }

    @Transactional(readOnly = true)
    // Finds by assignee id.
    public List<Task> findByAssigneeId(Long assigneeId) {
        return taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(assigneeId);
    }

    /** Root tasks only (all rows in `task` are roots; subtasks are in `subtask`). */
    @Transactional(readOnly = true)
    // Finds root tasks by project.
    public List<Task> findRootTasksByProject(Long projectId) {
        return taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId);
    }

    @Transactional(readOnly = true)
    // Finds due soon.
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
    // Returns board by project.
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
    // Returns overdue tasks.
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
    // Returns stats by project.
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
    // Returns stats by freelancer.
    public TaskStatsDto getStatsByFreelancer(Long freelancerId, Optional<LocalDate> from, Optional<LocalDate> to) {
        var spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.of(freelancerId),
                Optional.empty(), Optional.empty(), Optional.empty(), from, to, Optional.empty());
        List<Task> tasks = taskRepository.findAll(spec);
        List<Subtask> subtasks = subtaskRepository.findByAssigneeId(freelancerId).stream()
                .filter(s -> matchesDateRange(s.getDueDate(), from, to))
                .toList();
        long total = tasks.size() + subtasks.size();
        long done = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count()
                + subtasks.stream().filter(s -> s.getStatus() == TaskStatus.DONE).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.IN_REVIEW).count()
                + subtasks.stream().filter(s -> s.getStatus() == TaskStatus.IN_PROGRESS || s.getStatus() == TaskStatus.IN_REVIEW).count();
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

    // Performs matches date range.
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

    // Checks whether overdue open.
    private static boolean isOverdueOpen(LocalDate due, TaskStatus status) {
        return isOverdueOpen(due, status, LocalDate.now());
    }

    // Checks whether overdue open.
    private static boolean isOverdueOpen(LocalDate due, TaskStatus status, LocalDate asOf) {
        if (due == null || status == TaskStatus.DONE || status == TaskStatus.CANCELLED) {
            return false;
        }
        return due.isBefore(asOf);
    }

    @Transactional(readOnly = true)
    // Returns dashboard stats.
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
    // Returns extended stats by project.
    public TaskStatsExtendedDto getExtendedStatsByProject(Long projectId) {
        return buildExtendedStatsForProject(projectId, LocalDate.now(), Optional.empty(), Optional.empty());
    }

    @Transactional(readOnly = true)
    public TaskStatsExtendedDto getExtendedStatsByFreelancer(
            Long freelancerId,
            Optional<LocalDate> dueDateFrom,
            Optional<LocalDate> dueDateTo,
            Optional<LocalDate> activityFrom,
            Optional<LocalDate> activityTo) {
        var spec = TaskSpecification.filtered(
                Optional.empty(), Optional.empty(), Optional.of(freelancerId),
                Optional.empty(), Optional.empty(), Optional.empty(), dueDateFrom, dueDateTo, Optional.empty());
        List<Task> tasks = taskRepository.findAll(spec);
        List<Subtask> subtasks = subtaskRepository.findByAssigneeId(freelancerId).stream()
                .filter(s -> matchesDateRange(s.getDueDate(), dueDateFrom, dueDateTo))
                .toList();
        return extendedFromTaskAndSubtaskLists(tasks, subtasks, LocalDate.now(), activityFrom, activityTo);
    }

    @Transactional(readOnly = true)
    // Returns extended stats dashboard.
    public TaskStatsExtendedDto getExtendedStatsDashboard() {
        return buildExtendedStatsGlobal(LocalDate.now(), Optional.empty(), Optional.empty());
    }

    /**
     * Builds extended stats for a weekly report: full scoped workload, activity counts for the inclusive week,
     * overdue as of {@code weekEnd}.
     */
    @Transactional(readOnly = true)
    public TaskStatsExtendedDto getExtendedStatsForWeeklyWindow(
            Optional<Long> projectId,
            Optional<Long> freelancerId,
            LocalDate weekStartInclusive,
            LocalDate weekEndInclusive) {
        LocalDate overdueAsOf = weekEndInclusive;
        Optional<LocalDate> activityFrom = Optional.of(weekStartInclusive);
        Optional<LocalDate> activityTo = Optional.of(weekEndInclusive);
        if (projectId.isPresent() && freelancerId.isPresent()) {
            List<Task> tasks = taskRepository.findByProjectIdOrderByOrderIndexAsc(projectId.get()).stream()
                    .filter(t -> freelancerId.get().equals(t.getAssigneeId()))
                    .toList();
            List<Subtask> subtasks = subtaskRepository.findByProjectIdOrderByParent_IdAscOrderIndexAsc(projectId.get()).stream()
                    .filter(s -> freelancerId.get().equals(s.getAssigneeId()))
                    .toList();
            return extendedFromTaskAndSubtaskLists(tasks, subtasks, overdueAsOf, activityFrom, activityTo);
        }
        if (projectId.isPresent()) {
            return buildExtendedStatsForProject(projectId.get(), overdueAsOf, activityFrom, activityTo);
        }
        if (freelancerId.isPresent()) {
            Long fid = freelancerId.get();
            var spec = TaskSpecification.filtered(
                    Optional.empty(), Optional.empty(), Optional.of(fid),
                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
            List<Task> tasks = taskRepository.findAll(spec);
            List<Subtask> subtasks = subtaskRepository.findByAssigneeId(fid);
            return extendedFromTaskAndSubtaskLists(tasks, subtasks, overdueAsOf, activityFrom, activityTo);
        }
        return buildExtendedStatsGlobal(overdueAsOf, activityFrom, activityTo);
    }

    public List<String> getHighPriorityOpenLinesForReport(
            Optional<Long> projectId,
            Optional<Long> freelancerId,
            int maxLines) {
        List<Task> tasks;
        List<Subtask> subtasks;
        if (projectId.isPresent() && freelancerId.isPresent()) {
            tasks = taskRepository.findHighPriorityOpenForProject(projectId.get()).stream()
                    .filter(t -> freelancerId.get().equals(t.getAssigneeId()))
                    .toList();
            subtasks = subtaskRepository.findHighPriorityOpenForProject(projectId.get()).stream()
                    .filter(s -> freelancerId.get().equals(s.getAssigneeId()))
                    .toList();
        } else if (projectId.isPresent()) {
            tasks = taskRepository.findHighPriorityOpenForProject(projectId.get());
            subtasks = subtaskRepository.findHighPriorityOpenForProject(projectId.get());
        } else if (freelancerId.isPresent()) {
            tasks = taskRepository.findHighPriorityOpenForAssignee(freelancerId.get());
            subtasks = subtaskRepository.findHighPriorityOpenForAssignee(freelancerId.get());
        } else {
            tasks = taskRepository.findHighPriorityOpenAll();
            subtasks = subtaskRepository.findHighPriorityOpenAll();
        }
        List<String> lines = new ArrayList<>();
        for (Task t : tasks) {
            if (lines.size() >= maxLines) {
                break;
            }
            lines.add(String.format("[Task] %s (%s)", t.getTitle() != null ? t.getTitle() : "#" + t.getId(), t.getPriority()));
        }
        for (Subtask s : subtasks) {
            if (lines.size() >= maxLines) {
                break;
            }
            lines.add(String.format("[Subtask] %s (%s)", s.getTitle() != null ? s.getTitle() : "#" + s.getId(), s.getPriority()));
        }
        return lines;
    }

    private TaskStatsExtendedDto buildExtendedStatsForProject(
            Long projectId,
            LocalDate overdueAsOf,
            Optional<LocalDate> activityFrom,
            Optional<LocalDate> activityTo) {
        Map<TaskStatus, Long> byStatus = mergedStatusCounts(
                taskRepository.countGroupByStatusForProject(projectId),
                subtaskRepository.countGroupByStatusForProject(projectId));
        Map<TaskPriority, Long> byPriority = mergedPriorityCounts(
                taskRepository.countGroupByPriorityForProject(projectId),
                subtaskRepository.countGroupByPriorityForProject(projectId));
        long taskTotal = taskRepository.countByProjectId(projectId);
        long subTotal = subtaskRepository.countByProjectId(projectId);
        long total = taskTotal + subTotal;
        long done = byStatus.getOrDefault(TaskStatus.DONE, 0L);
        long overdue = taskRepository.findOverdueTasksByProject(projectId, overdueAsOf).size()
                + subtaskRepository.findOverdueSubtasksByProject(projectId, overdueAsOf).size();
        long unassigned = taskRepository.countByProjectIdAndAssigneeIdIsNull(projectId)
                + subtaskRepository.countByProjectIdAndAssigneeIdIsNull(projectId);
        long[] range = activityRangeCountsForProject(projectId, activityFrom, activityTo);
        return toExtendedDto(byStatus, byPriority, total, done, overdue, unassigned, range[0], range[1], List.of());
    }

    private TaskStatsExtendedDto buildExtendedStatsGlobal(
            LocalDate overdueAsOf,
            Optional<LocalDate> activityFrom,
            Optional<LocalDate> activityTo) {
        Map<TaskStatus, Long> byStatus = mergedStatusCounts(
                taskRepository.countGroupByStatusAll(),
                subtaskRepository.countGroupByStatusAll());
        Map<TaskPriority, Long> byPriority = mergedPriorityCounts(
                taskRepository.countGroupByPriorityAll(),
                subtaskRepository.countGroupByPriorityAll());
        long taskTotal = taskRepository.count();
        long subTotal = subtaskRepository.count();
        long total = taskTotal + subTotal;
        long done = byStatus.getOrDefault(TaskStatus.DONE, 0L);
        long overdue = taskRepository.findOverdueTasks(overdueAsOf).size()
                + subtaskRepository.findOverdueSubtasks(overdueAsOf).size();
        long unassigned = taskRepository.countByAssigneeIdIsNull() + subtaskRepository.countByAssigneeIdIsNull();
        long[] range = activityRangeCountsGlobal(activityFrom, activityTo);
        return toExtendedDto(byStatus, byPriority, total, done, overdue, unassigned, range[0], range[1], List.of());
    }

    private long[] activityRangeCountsForProject(
            Long projectId,
            Optional<LocalDate> activityFrom,
            Optional<LocalDate> activityTo) {
        if (activityFrom.isEmpty() || activityTo.isEmpty()) {
            return new long[] {0, 0};
        }
        LocalDateTime start = activityFrom.get().atStartOfDay();
        LocalDateTime endExclusive = activityTo.get().plusDays(1).atStartOfDay();
        long created = taskRepository.countCreatedInRangeForProject(projectId, start, endExclusive)
                + subtaskRepository.countCreatedInRangeForProject(projectId, start, endExclusive);
        long completed = taskRepository.countCompletedInRangeForProject(projectId, start, endExclusive)
                + subtaskRepository.countCompletedInRangeForProject(projectId, start, endExclusive);
        return new long[] {created, completed};
    }

    // Performs activity range counts global.
    private long[] activityRangeCountsGlobal(Optional<LocalDate> activityFrom, Optional<LocalDate> activityTo) {
        if (activityFrom.isEmpty() || activityTo.isEmpty()) {
            return new long[] {0, 0};
        }
        LocalDateTime start = activityFrom.get().atStartOfDay();
        LocalDateTime endExclusive = activityTo.get().plusDays(1).atStartOfDay();
        long created = taskRepository.countCreatedInRangeAll(start, endExclusive)
                + subtaskRepository.countCreatedInRangeAll(start, endExclusive);
        long completed = taskRepository.countCompletedInRangeAll(start, endExclusive)
                + subtaskRepository.countCompletedInRangeAll(start, endExclusive);
        return new long[] {created, completed};
    }

    // Performs merged status counts.
    private static Map<TaskStatus, Long> mergedStatusCounts(List<Object[]> taskRows, List<Object[]> subRows) {
        Map<TaskStatus, Long> map = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            map.put(s, 0L);
        }
        accumulateStatusRows(map, taskRows);
        accumulateStatusRows(map, subRows);
        return map;
    }

    // Performs accumulate status rows.
    private static void accumulateStatusRows(Map<TaskStatus, Long> map, List<Object[]> rows) {
        for (Object[] row : rows) {
            TaskStatus st = (TaskStatus) row[0];
            long c = ((Number) row[1]).longValue();
            map.merge(st, c, Long::sum);
        }
    }

    // Performs merged priority counts.
    private static Map<TaskPriority, Long> mergedPriorityCounts(List<Object[]> taskRows, List<Object[]> subRows) {
        Map<TaskPriority, Long> map = new EnumMap<>(TaskPriority.class);
        for (TaskPriority p : TaskPriority.values()) {
            map.put(p, 0L);
        }
        accumulatePriorityRows(map, taskRows);
        accumulatePriorityRows(map, subRows);
        return map;
    }

    // Performs accumulate priority rows.
    private static void accumulatePriorityRows(Map<TaskPriority, Long> map, List<Object[]> rows) {
        for (Object[] row : rows) {
            TaskPriority p = (TaskPriority) row[0];
            long c = ((Number) row[1]).longValue();
            map.merge(p, c, Long::sum);
        }
    }

    private static TaskStatsExtendedDto toExtendedDto(
            Map<TaskStatus, Long> byStatus,
            Map<TaskPriority, Long> byPriority,
            long total,
            long done,
            long overdue,
            long unassigned,
            long createdInRange,
            long completedInRange,
            List<Long> projectIdsWithAssignedWork) {
        List<TaskPriorityCountDto> breakdown = Arrays.stream(TaskPriority.values())
                .map(p -> TaskPriorityCountDto.builder()
                        .priority(p)
                        .count(byPriority.getOrDefault(p, 0L))
                        .build())
                .toList();
        double pct = total > 0 ? (100.0 * done / total) : 0.0;
        List<Long> projectIds = projectIdsWithAssignedWork == null
                ? new ArrayList<>()
                : new ArrayList<>(projectIdsWithAssignedWork);
        return TaskStatsExtendedDto.builder()
                .totalTasks(total)
                .doneCount(done)
                .inProgressCount(byStatus.getOrDefault(TaskStatus.IN_PROGRESS, 0L))
                .inReviewCount(byStatus.getOrDefault(TaskStatus.IN_REVIEW, 0L))
                .todoCount(byStatus.getOrDefault(TaskStatus.TODO, 0L))
                .cancelledCount(byStatus.getOrDefault(TaskStatus.CANCELLED, 0L))
                .overdueCount(overdue)
                .completionPercentage(pct)
                .unassignedCount(unassigned)
                .createdInRangeCount(createdInRange)
                .completedInRangeCount(completedInRange)
                .priorityBreakdown(new ArrayList<>(breakdown))
                .projectIdsWithAssignedWork(projectIds)
                .build();
    }

    private TaskStatsExtendedDto extendedFromTaskAndSubtaskLists(
            List<Task> tasks,
            List<Subtask> subtasks,
            LocalDate overdueAsOf,
            Optional<LocalDate> activityFrom,
            Optional<LocalDate> activityTo) {
        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, 0L);
        }
        Map<TaskPriority, Long> byPriority = new EnumMap<>(TaskPriority.class);
        for (TaskPriority p : TaskPriority.values()) {
            byPriority.put(p, 0L);
        }
        long unassigned = 0;
        for (Task t : tasks) {
            byStatus.merge(t.getStatus(), 1L, Long::sum);
            byPriority.merge(t.getPriority(), 1L, Long::sum);
            if (t.getAssigneeId() == null) {
                unassigned++;
            }
        }
        for (Subtask s : subtasks) {
            byStatus.merge(s.getStatus(), 1L, Long::sum);
            byPriority.merge(s.getPriority(), 1L, Long::sum);
            if (s.getAssigneeId() == null) {
                unassigned++;
            }
        }
        long total = tasks.size() + subtasks.size();
        long done = byStatus.getOrDefault(TaskStatus.DONE, 0L);
        long overdue = tasks.stream().filter(t -> isOverdueOpen(t.getDueDate(), t.getStatus(), overdueAsOf)).count()
                + subtasks.stream().filter(s -> isOverdueOpen(s.getDueDate(), s.getStatus(), overdueAsOf)).count();
        long createdInRange = 0;
        long completedInRange = 0;
        if (activityFrom.isPresent() && activityTo.isPresent()) {
            LocalDateTime start = activityFrom.get().atStartOfDay();
            LocalDateTime endExclusive = activityTo.get().plusDays(1).atStartOfDay();
            createdInRange = tasks.stream()
                    .filter(t -> !t.getCreatedAt().isBefore(start) && t.getCreatedAt().isBefore(endExclusive))
                    .count()
                    + subtasks.stream()
                    .filter(s -> !s.getCreatedAt().isBefore(start) && s.getCreatedAt().isBefore(endExclusive))
                    .count();
            completedInRange = tasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE
                            && !t.getUpdatedAt().isBefore(start) && t.getUpdatedAt().isBefore(endExclusive))
                    .count()
                    + subtasks.stream()
                    .filter(s -> s.getStatus() == TaskStatus.DONE
                            && !s.getUpdatedAt().isBefore(start) && s.getUpdatedAt().isBefore(endExclusive))
                    .count();
        }
        SortedSet<Long> projectIds = new TreeSet<>();
        for (Task t : tasks) {
            if (t.getProjectId() != null) {
                projectIds.add(t.getProjectId());
            }
        }
        for (Subtask s : subtasks) {
            if (s.getProjectId() != null) {
                projectIds.add(s.getProjectId());
            }
        }
        return toExtendedDto(
                byStatus, byPriority, total, done, overdue, unassigned, createdInRange, completedInRange,
                new ArrayList<>(projectIds));
    }

    @Transactional(readOnly = true)
    // Returns calendar events.
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
    // Creates this operation.
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
    // Updates this operation.
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
    // Partially updates status.
    public Task patchStatus(Long id, TaskStatus status) {
        Task task = findById(id);
        task.setStatus(status);
        Task saved = taskRepository.save(task);
        taskNotificationService.notifyTaskStatusUpdate(saved);
        return saved;
    }

    @Transactional
    // Partially updates assignee.
    public Task patchAssignee(Long id, Long assigneeId) {
        Task task = findById(id);
        task.setAssigneeId(assigneeId);
        return taskRepository.save(task);
    }

    @Transactional
    // Partially updates due date.
    public Task patchDueDate(Long id, LocalDate dueDate) {
        Task task = findById(id);
        task.setDueDate(dueDate);
        return taskRepository.save(task);
    }

    @Transactional
    // Performs bulk patch status.
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
    // Performs reorder.
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
    // Deletes by id.
    public void deleteById(Long id) {
        Task task = findById(id);
        taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(id).forEach(taskCommentRepository::delete);
        taskRepository.delete(task);
    }

    @Transactional
    // Performs escalate overdue priorities.
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

    /**
     * Notifies each freelancer who has overdue root tasks or subtasks assigned to them.
     * Runs daily via scheduler; sends one in-app notification per assignee with a summary list.
     *
     * @return number of assignees notified
     */
    @Transactional(readOnly = true)
    // Sends daily overdue reminders.
    public int sendDailyOverdueReminders() {
        LocalDate today = LocalDate.now();
        List<Task> overdueRoots = taskRepository.findOverdueTasks(today).stream()
                .filter(t -> t.getAssigneeId() != null)
                .toList();
        List<Subtask> overdueSubs = subtaskRepository.findOverdueSubtasks(today).stream()
                .filter(s -> s.getAssigneeId() != null)
                .toList();
        if (overdueRoots.isEmpty() && overdueSubs.isEmpty()) {
            return 0;
        }
        Map<Long, List<String>> linesByAssignee = new LinkedHashMap<>();
        for (Task t : overdueRoots) {
            String title = t.getTitle() != null ? t.getTitle() : "Task #" + t.getId();
            String line = String.format("Task: %s — due %s", title, t.getDueDate());
            linesByAssignee.computeIfAbsent(t.getAssigneeId(), k -> new ArrayList<>()).add(line);
        }
        for (Subtask s : overdueSubs) {
            String title = s.getTitle() != null ? s.getTitle() : "Subtask #" + s.getId();
            String line = String.format("Subtask: %s — due %s", title, s.getDueDate());
            linesByAssignee.computeIfAbsent(s.getAssigneeId(), k -> new ArrayList<>()).add(line);
        }
        int notified = 0;
        for (Map.Entry<Long, List<String>> e : linesByAssignee.entrySet()) {
            List<String> lines = new ArrayList<>(e.getValue());
            Collections.sort(lines);
            taskNotificationService.notifyFreelancerDailyOverdueReminder(e.getKey(), lines);
            notified++;
        }
        return notified;
    }

    @Transactional
    // Performs purge old cancelled tasks.
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
    // Returns subtask progress for assignee tasks.
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
    // Returns project activity for assignee.
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

    // Performs task view from subtask.
    private Task taskViewFromSubtask(Subtask s) {
        Task t = new Task();
        t.setSubtask(true);
        t.setId(s.getId());
        if (s.getParent() != null) {
            t.setParentTaskId(s.getParent().getId());
        }
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
