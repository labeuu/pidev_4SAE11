package com.esprit.task.service;

import com.esprit.task.client.ProjectClient;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.esprit.task.dto.ProjectActivityDto;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.SubtaskProgressDto;
import com.esprit.task.dto.TaskCalendarEventDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SubtaskRepository subtaskRepository;

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private ProjectClient projectClient;

    @Mock
    private TaskNotificationService taskNotificationService;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void lenientSubtaskStubs() {
        lenient().when(subtaskRepository.findDueSoonSubtasks(any(), any())).thenReturn(List.of());
        lenient().when(subtaskRepository.findDueSoonSubtasksByProject(any(), any(), anyLong())).thenReturn(List.of());
        lenient().when(subtaskRepository.findDueSoonSubtasksByAssignee(any(), any(), anyLong())).thenReturn(List.of());
        lenient().when(subtaskRepository.findByAssigneeId(anyLong())).thenReturn(List.of());
        lenient().when(subtaskRepository.countByProjectId(anyLong())).thenReturn(0L);
        lenient().when(subtaskRepository.countByProjectIdAndStatus(anyLong(), any())).thenReturn(0L);
        lenient().when(subtaskRepository.findOverdueSubtasksByProject(anyLong(), any())).thenReturn(List.of());
        lenient().when(subtaskRepository.count()).thenReturn(0L);
        lenient().when(subtaskRepository.findAll()).thenReturn(List.of());
        lenient().when(subtaskRepository.findOverdueSubtasks(any())).thenReturn(List.of());
        lenient().when(subtaskRepository.findOverdueSubtasksByAssignee(anyLong(), any())).thenReturn(List.of());
        lenient().when(subtaskRepository.findByDueDateBetween(any(), any())).thenReturn(List.of());
        lenient().when(subtaskRepository.findByDueDateBetweenAndAssigneeId(any(), any(), anyLong())).thenReturn(List.of());
    }

    private static Task task(Long id) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(1L);
        t.setTitle("Task " + id);
        t.setStatus(TaskStatus.TODO);
        t.setPriority(TaskPriority.MEDIUM);
        t.setDueDate(LocalDate.now().plusDays(7));
        return t;
    }

    @Test
    void findById_whenFound_returnsTask() {
        Task t = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));

        Task result = taskService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Task 1");
    }

    @Test
    void findById_whenNotFound_throws() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void findByProjectId_returnsList() {
        Task t = task(1L);
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(t));

        List<Task> result = taskService.findByProjectId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void getBoardByProject_returnsBoard() {
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(task(1L)));

        var result = taskService.getBoardByProject(1L);

        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getColumns()).isNotNull();
    }

    @Test
    void getOverdueTasks_returnsList() {
        when(taskRepository.findOverdueTasks(LocalDate.now())).thenReturn(List.of(task(1L)));

        List<Task> result = taskService.getOverdueTasks(Optional.empty(), Optional.empty());

        assertThat(result).hasSize(1);
    }

    @Test
    void getStatsByProject_returnsStats() {
        when(taskRepository.countByProjectId(1L)).thenReturn(10L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.DONE)).thenReturn(5L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_REVIEW)).thenReturn(1L);
        when(taskRepository.findOverdueTasksByProject(1L, LocalDate.now())).thenReturn(List.of());

        TaskStatsDto result = taskService.getStatsByProject(1L);

        assertThat(result.getTotalTasks()).isEqualTo(10);
        assertThat(result.getDoneCount()).isEqualTo(5);
        assertThat(result.getCompletionPercentage()).isEqualTo(50.0);
    }

    @Test
    void create_setsDefaultsAndSaves() {
        Task t = task(null);
        t.setStatus(null);
        t.setPriority(null);
        t.setOrderIndex(null);
        when(taskRepository.findMaxOrderIndexByProject(1L)).thenReturn(0);
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Task result = taskService.create(t);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any());
    }

    @Test
    void update_modifiesAndSaves() {
        Task existing = task(1L);
        Task updated = task(1L);
        updated.setTitle("Updated");
        updated.setStatus(TaskStatus.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any())).thenReturn(updated);

        Task result = taskService.update(1L, updated);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any());
        verify(taskNotificationService, never()).notifyTaskStatusUpdate(any());
    }

    @Test
    void update_whenStatusChanged_notifiesClient() {
        Task existing = task(1L);
        existing.setStatus(TaskStatus.TODO);
        Task updated = task(1L);
        updated.setTitle("Updated");
        updated.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task s = inv.getArgument(0);
            s.setStatus(TaskStatus.IN_PROGRESS);
            return s;
        });

        taskService.update(1L, updated);

        verify(taskNotificationService).notifyTaskStatusUpdate(any());
    }

    @Test
    void patchStatus_updatesStatusAndNotifies() {
        Task t = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setStatus(TaskStatus.IN_PROGRESS);
            return saved;
        });

        taskService.patchStatus(1L, TaskStatus.IN_PROGRESS);

        verify(taskRepository).save(any());
        verify(taskNotificationService).notifyTaskStatusUpdate(any());
    }

    @Test
    void findAllFiltered_returnsPaginatedResults() {
        Task t = task(1L);
        Page<Task> page = new PageImpl<>(List.of(t), PageRequest.of(0, 20), 1);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Task> result = taskService.findAllFiltered(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(),
                PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findByContractId_returnsList() {
        Task t = task(1L);
        when(taskRepository.findByContractIdOrderByProjectIdAscOrderIndexAsc(1L)).thenReturn(List.of(t));

        List<Task> result = taskService.findByContractId(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByAssigneeId_returnsList() {
        Task t = task(1L);
        t.setAssigneeId(10L);
        when(taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(10L)).thenReturn(List.of(t));

        List<Task> result = taskService.findByAssigneeId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssigneeId()).isEqualTo(10L);
    }

    @Test
    void getStatsByFreelancer_returnsStats() {
        Task t = task(1L);
        t.setStatus(TaskStatus.DONE);
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(t, task(2L)));

        TaskStatsDto result = taskService.getStatsByFreelancer(10L, Optional.empty(), Optional.empty());

        assertThat(result.getTotalTasks()).isEqualTo(2);
        assertThat(result.getDoneCount()).isEqualTo(1);
    }

    @Test
    void getDashboardStats_returnsStats() {
        when(taskRepository.findAll()).thenReturn(List.of(task(1L), task(2L)));
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of());

        TaskStatsDto result = taskService.getDashboardStats();

        assertThat(result.getTotalTasks()).isEqualTo(2);
    }

    @Test
    void patchAssignee_updatesAssignee() {
        Task t = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenReturn(t);

        taskService.patchAssignee(1L, 10L);

        verify(taskRepository).save(any());
    }

    @Test
    void reorder_updatesOrderIndex() {
        Task t = task(1L);
        t.setOrderIndex(5);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenReturn(t);

        taskService.reorder(List.of(1L));

        verify(taskRepository).save(any());
    }

    @Test
    void create_whenMaxOrderIndexNull_setsOrderIndexZero() {
        Task t = task(null);
        t.setStatus(null);
        t.setPriority(null);
        t.setOrderIndex(null);
        when(taskRepository.findMaxOrderIndexByProject(1L)).thenReturn(null);
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task saved = inv.getArgument(0);
            saved.setId(1L);
            saved.setOrderIndex(0);
            return saved;
        });

        Task result = taskService.create(t);

        assertThat(result).isNotNull();
        verify(taskRepository).save(any());
    }

    @Test
    void deleteById_deletesCommentsAndTask() {
        Task t = task(1L);
        com.esprit.task.entity.TaskComment c = new com.esprit.task.entity.TaskComment();
        c.setId(1L);
        c.setTaskId(1L);
        c.setUserId(10L);
        c.setMessage("x");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(c));

        taskService.deleteById(1L);

        verify(taskCommentRepository).delete(c);
        verify(taskRepository).delete(t);
    }

    @Test
    void getCalendarEvents_withoutUserId_returnsEventsFromRepo() {
        Task t = task(1L);
        t.setDueDate(LocalDate.now().plusDays(1));
        LocalDateTime min = LocalDateTime.now();
        LocalDateTime max = min.plusMonths(1);
        when(taskRepository.findByDueDateBetween(min.toLocalDate(), max.toLocalDate()))
                .thenReturn(List.of(t));

        List<TaskCalendarEventDto> result = taskService.getCalendarEvents(min, max, Optional.empty());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("task-1");
        assertThat(result.get(0).getSummary()).contains("Task 1");
    }

    @Test
    void getCalendarEvents_withUserId_returnsMergedAssigneeAndProjectTasks() {
        Task t1 = task(1L);
        t1.setAssigneeId(5L);
        t1.setDueDate(LocalDate.now().plusDays(1));
        Task t2 = task(2L);
        t2.setProjectId(10L);
        t2.setDueDate(LocalDate.now().plusDays(2));
        LocalDateTime min = LocalDateTime.now();
        LocalDateTime max = min.plusMonths(1);
        LocalDate start = min.toLocalDate();
        LocalDate end = max.toLocalDate();

        when(projectClient.getProjectsByClientId(5L)).thenReturn(
                List.of(new ProjectDto(10L, 5L, "P", null, null)));
        when(taskRepository.findByDueDateBetweenAndAssigneeId(start, end, 5L)).thenReturn(List.of(t1));
        when(taskRepository.findByDueDateBetween(start, end)).thenReturn(List.of(t1, t2));

        List<TaskCalendarEventDto> result = taskService.getCalendarEvents(min, max, Optional.of(5L));

        assertThat(result).hasSize(2);
    }

    @Test
    void getCalendarEvents_withUserId_whenProjectClientThrows_continuesWithoutProjectIds() {
        Task t = task(1L);
        t.setAssigneeId(5L);
        t.setDueDate(LocalDate.now().plusDays(1));
        LocalDateTime min = LocalDateTime.now();
        LocalDateTime max = min.plusMonths(1);
        LocalDate start = min.toLocalDate();
        LocalDate end = max.toLocalDate();

        when(projectClient.getProjectsByClientId(5L)).thenThrow(new RuntimeException("Network error"));
        when(taskRepository.findByDueDateBetweenAndAssigneeId(start, end, 5L)).thenReturn(List.of(t));

        List<TaskCalendarEventDto> result = taskService.getCalendarEvents(min, max, Optional.of(5L));

        assertThat(result).hasSize(1);
    }

    @Test
    void getOverdueTasks_withProjectId_only_returnsFiltered() {
        Task t = task(1L);
        LocalDate today = LocalDate.now();
        when(taskRepository.findOverdueTasksByProject(1L, today)).thenReturn(List.of(t));

        List<Task> result = taskService.getOverdueTasks(Optional.of(1L), Optional.empty());

        assertThat(result).hasSize(1);
    }

    @Test
    void getOverdueTasks_withAssigneeId_only_returnsFiltered() {
        Task t = task(1L);
        t.setAssigneeId(10L);
        LocalDate today = LocalDate.now();
        when(taskRepository.findOverdueTasksByAssignee(10L, today)).thenReturn(List.of(t));

        List<Task> result = taskService.getOverdueTasks(Optional.empty(), Optional.of(10L));

        assertThat(result).hasSize(1);
    }

    @Test
    void getOverdueTasks_withBothProjectAndAssignee_filtersByBoth() {
        Task t = task(1L);
        t.setAssigneeId(10L);
        LocalDate today = LocalDate.now();
        when(taskRepository.findOverdueTasksByProject(1L, today)).thenReturn(List.of(t));

        List<Task> result = taskService.getOverdueTasks(Optional.of(1L), Optional.of(10L));

        assertThat(result).hasSize(1);
    }

    @Test
    void getStatsByProject_whenTotalZero_returnsZeroCompletion() {
        when(taskRepository.countByProjectId(1L)).thenReturn(0L);
        when(taskRepository.findOverdueTasksByProject(1L, LocalDate.now())).thenReturn(List.of());

        TaskStatsDto result = taskService.getStatsByProject(1L);

        assertThat(result.getTotalTasks()).isZero();
        assertThat(result.getCompletionPercentage()).isEqualTo(0.0);
    }

    @Test
    void getExtendedStatsByProject_returnsBreakdown() {
        List<Object[]> taskStatuses = new ArrayList<>();
        taskStatuses.add(new Object[] {TaskStatus.TODO, 2L});
        taskStatuses.add(new Object[] {TaskStatus.DONE, 1L});
        taskStatuses.add(new Object[] {TaskStatus.IN_PROGRESS, 1L});
        when(taskRepository.countGroupByStatusForProject(1L)).thenReturn(taskStatuses);
        List<Object[]> subStatuses = new ArrayList<>();
        subStatuses.add(new Object[] {TaskStatus.IN_REVIEW, 1L});
        when(subtaskRepository.countGroupByStatusForProject(1L)).thenReturn(subStatuses);
        List<Object[]> taskPri = new ArrayList<>();
        taskPri.add(new Object[] {TaskPriority.HIGH, 3L});
        taskPri.add(new Object[] {TaskPriority.MEDIUM, 2L});
        when(taskRepository.countGroupByPriorityForProject(1L)).thenReturn(taskPri);
        when(subtaskRepository.countGroupByPriorityForProject(1L)).thenReturn(List.of());
        when(taskRepository.countByProjectId(1L)).thenReturn(4L);
        when(subtaskRepository.countByProjectId(1L)).thenReturn(1L);
        when(taskRepository.countByProjectIdAndAssigneeIdIsNull(1L)).thenReturn(1L);
        when(subtaskRepository.countByProjectIdAndAssigneeIdIsNull(1L)).thenReturn(0L);
        when(taskRepository.findOverdueTasksByProject(eq(1L), any(LocalDate.class))).thenReturn(List.of());
        when(subtaskRepository.findOverdueSubtasksByProject(eq(1L), any(LocalDate.class))).thenReturn(List.of());

        TaskStatsExtendedDto r = taskService.getExtendedStatsByProject(1L);

        assertThat(r.getTotalTasks()).isEqualTo(5);
        assertThat(r.getDoneCount()).isEqualTo(1);
        assertThat(r.getTodoCount()).isEqualTo(2);
        assertThat(r.getInProgressCount()).isEqualTo(1);
        assertThat(r.getInReviewCount()).isEqualTo(1);
        assertThat(r.getPriorityBreakdown()).hasSize(4);
        assertThat(r.getUnassignedCount()).isEqualTo(1);
        assertThat(r.getCreatedInRangeCount()).isZero();
    }

    @Test
    void getExtendedStatsForWeeklyWindow_setsActivityCountsForProject() {
        LocalDate mon = LocalDate.of(2026, 4, 6);
        LocalDate sun = mon.plusDays(6);
        List<Object[]> doneOnly = new ArrayList<>();
        doneOnly.add(new Object[] {TaskStatus.DONE, 1L});
        when(taskRepository.countGroupByStatusForProject(1L)).thenReturn(doneOnly);
        when(subtaskRepository.countGroupByStatusForProject(1L)).thenReturn(List.of());
        when(taskRepository.countGroupByPriorityForProject(1L)).thenReturn(List.of());
        when(subtaskRepository.countGroupByPriorityForProject(1L)).thenReturn(List.of());
        when(taskRepository.countByProjectId(1L)).thenReturn(1L);
        when(subtaskRepository.countByProjectId(1L)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndAssigneeIdIsNull(1L)).thenReturn(0L);
        when(subtaskRepository.countByProjectIdAndAssigneeIdIsNull(1L)).thenReturn(0L);
        when(taskRepository.findOverdueTasksByProject(eq(1L), eq(sun))).thenReturn(List.of());
        when(subtaskRepository.findOverdueSubtasksByProject(eq(1L), eq(sun))).thenReturn(List.of());
        LocalDateTime start = mon.atStartOfDay();
        LocalDateTime endEx = sun.plusDays(1).atStartOfDay();
        when(taskRepository.countCreatedInRangeForProject(eq(1L), eq(start), eq(endEx))).thenReturn(2L);
        when(subtaskRepository.countCreatedInRangeForProject(eq(1L), eq(start), eq(endEx))).thenReturn(0L);
        when(taskRepository.countCompletedInRangeForProject(eq(1L), eq(start), eq(endEx))).thenReturn(1L);
        when(subtaskRepository.countCompletedInRangeForProject(eq(1L), eq(start), eq(endEx))).thenReturn(0L);

        TaskStatsExtendedDto r = taskService.getExtendedStatsForWeeklyWindow(Optional.of(1L), Optional.empty(), mon, sun);

        assertThat(r.getCreatedInRangeCount()).isEqualTo(2);
        assertThat(r.getCompletedInRangeCount()).isEqualTo(1);
    }

    @Test
    void getExtendedStatsForWeeklyWindow_freelancer_loadsAssigneeTasks() {
        LocalDate mon = LocalDate.of(2026, 4, 6);
        LocalDate sun = mon.plusDays(6);
        Task t = task(1L);
        t.setAssigneeId(9L);
        t.setStatus(TaskStatus.TODO);
        t.setCreatedAt(mon.atStartOfDay());
        t.setUpdatedAt(mon.atStartOfDay());
        when(taskRepository.findAll(any(Specification.class))).thenReturn(List.of(t));
        when(subtaskRepository.findByAssigneeId(9L)).thenReturn(List.of());

        TaskStatsExtendedDto r = taskService.getExtendedStatsForWeeklyWindow(Optional.empty(), Optional.of(9L), mon, sun);

        assertThat(r.getTotalTasks()).isEqualTo(1);
        assertThat(r.getTodoCount()).isEqualTo(1);
    }

    @Test
    void deleteById_deletesRootOnly_subtasksCascadeAtPersistenceLayer() {
        Task parent = task(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        taskService.deleteById(1L);

        verify(taskRepository).delete(parent);
        verify(taskRepository, times(1)).delete(any(Task.class));
    }

    @Test
    void escalateOverduePriorities_setsHighAndNotifiesAssignee() {
        Task overdue = task(1L);
        overdue.setPriority(TaskPriority.LOW);
        overdue.setDueDate(LocalDate.now().minusDays(1));
        overdue.setAssigneeId(5L);
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of(overdue));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int n = taskService.escalateOverduePriorities();

        assertThat(n).isEqualTo(1);
        assertThat(overdue.getPriority()).isEqualTo(TaskPriority.HIGH);
        verify(taskNotificationService).notifyTaskPriorityEscalated(overdue);
    }

    @Test
    void escalateOverduePriorities_withoutAssignee_skipsNotification() {
        Task overdue = task(1L);
        overdue.setPriority(TaskPriority.MEDIUM);
        overdue.setDueDate(LocalDate.now().minusDays(1));
        overdue.setAssigneeId(null);
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of(overdue));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskService.escalateOverduePriorities();

        verify(taskNotificationService, never()).notifyTaskPriorityEscalated(any());
        assertThat(overdue.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void escalateOverduePriorities_whenAlreadyHigh_doesNotEscalateOrNotify() {
        Task overdue = task(1L);
        overdue.setPriority(TaskPriority.HIGH);
        overdue.setDueDate(LocalDate.now().minusDays(1));
        overdue.setAssigneeId(5L);
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of(overdue));

        int n = taskService.escalateOverduePriorities();

        assertThat(n).isZero();
        verify(taskRepository, never()).save(any());
        verify(taskNotificationService, never()).notifyTaskPriorityEscalated(any());
    }

    @Test
    void sendDailyOverdueReminders_groupsByAssignee_andNotifiesOnce() {
        Task t1 = task(1L);
        t1.setTitle("Root A");
        t1.setAssigneeId(5L);
        t1.setDueDate(LocalDate.now().minusDays(1));
        Subtask s = new Subtask();
        s.setId(99L);
        s.setProjectId(1L);
        s.setTitle("Sub B");
        s.setStatus(TaskStatus.TODO);
        s.setPriority(TaskPriority.MEDIUM);
        s.setAssigneeId(5L);
        s.setDueDate(LocalDate.now().minusDays(3));
        s.setOrderIndex(0);
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of(t1));
        when(subtaskRepository.findOverdueSubtasks(any(LocalDate.class))).thenReturn(List.of(s));

        int n = taskService.sendDailyOverdueReminders();

        assertThat(n).isEqualTo(1);
        verify(taskNotificationService, times(1)).notifyFreelancerDailyOverdueReminder(eq(5L), anyList());
    }

    @Test
    void sendDailyOverdueReminders_whenNone_returnsZero() {
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of());
        when(subtaskRepository.findOverdueSubtasks(any(LocalDate.class))).thenReturn(List.of());

        assertThat(taskService.sendDailyOverdueReminders()).isZero();
        verify(taskNotificationService, never()).notifyFreelancerDailyOverdueReminder(anyLong(), anyList());
    }

    @Test
    void sendDailyOverdueReminders_skipsUnassignedRoots() {
        Task unassigned = task(1L);
        unassigned.setTitle("No one");
        unassigned.setAssigneeId(null);
        unassigned.setDueDate(LocalDate.now().minusDays(1));
        when(taskRepository.findOverdueTasks(any(LocalDate.class))).thenReturn(List.of(unassigned));
        when(subtaskRepository.findOverdueSubtasks(any(LocalDate.class))).thenReturn(List.of());

        assertThat(taskService.sendDailyOverdueReminders()).isZero();
        verify(taskNotificationService, never()).notifyFreelancerDailyOverdueReminder(anyLong(), anyList());
    }

    @Test
    void purgeOldCancelledTasks_deletesWhenExists() {
        Task old = task(1L);
        old.setStatus(TaskStatus.CANCELLED);
        old.setUpdatedAt(LocalDateTime.now().minusDays(100));
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        when(taskRepository.findByStatusAndUpdatedAtBefore(TaskStatus.CANCELLED, cutoff)).thenReturn(List.of(old));
        when(taskRepository.existsById(1L)).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(old));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        int n = taskService.purgeOldCancelledTasks(cutoff);

        assertThat(n).isEqualTo(1);
        verify(taskRepository).delete(old);
    }

    @Test
    void purgeOldCancelledTasks_skipsWhenAlreadyRemoved() {
        Task old = task(1L);
        old.setStatus(TaskStatus.CANCELLED);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        when(taskRepository.findByStatusAndUpdatedAtBefore(TaskStatus.CANCELLED, cutoff)).thenReturn(List.of(old));
        when(taskRepository.existsById(1L)).thenReturn(false);

        int n = taskService.purgeOldCancelledTasks(cutoff);

        assertThat(n).isZero();
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void findRootTasksByProject_returnsFromRepository() {
        Task t = task(1L);
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(9L)).thenReturn(List.of(t));

        List<Task> result = taskService.findRootTasksByProject(9L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void findDueSoon_clampsWithinDaysAndQueries() {
        when(taskRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(task(1L)));

        List<Task> result = taskService.findDueSoon(Optional.of(3L), Optional.empty(), 999);

        assertThat(result).hasSize(1);
        verify(taskRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void findDueSoon_clampsLowBound() {
        when(taskRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        taskService.findDueSoon(Optional.empty(), Optional.empty(), 0);

        verify(taskRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void patchDueDate_updatesField() {
        Task t = task(1L);
        LocalDate d = LocalDate.now().plusWeeks(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.patchDueDate(1L, d);

        assertThat(result.getDueDate()).isEqualTo(d);
    }

    @Test
    void bulkPatchStatus_notifiesPerChangedStatus() {
        Task a = task(1L);
        a.setStatus(TaskStatus.TODO);
        Task b = task(2L);
        b.setStatus(TaskStatus.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(a));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(b));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskService.bulkPatchStatus(List.of(1L, 2L), TaskStatus.IN_PROGRESS);

        verify(taskNotificationService, times(2)).notifyTaskStatusUpdate(any());
    }

    @Test
    void bulkPatchStatus_whenStatusUnchanged_skipsNotification() {
        Task a = task(1L);
        a.setStatus(TaskStatus.DONE);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(a));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        taskService.bulkPatchStatus(List.of(1L), TaskStatus.DONE);

        verify(taskNotificationService, never()).notifyTaskStatusUpdate(any());
    }

    @Test
    void reorder_whenTaskIdsNull_throws() {
        assertThatThrownBy(() -> taskService.reorder(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void getSubtaskProgressForAssigneeTasks_whenEmpty_skipsRepository() {
        assertThat(taskService.getSubtaskProgressForAssigneeTasks(1L, List.of())).isEmpty();
        verify(subtaskRepository, never()).aggregateSubtaskProgressByParent(anyLong(), anyList());
    }

    @Test
    void getSubtaskProgressForAssigneeTasks_mapsRows() {
        when(subtaskRepository.aggregateSubtaskProgressByParent(5L, List.of(10L)))
                .thenReturn(Collections.singletonList(new Object[] {10L, 3L, 1L}));

        List<SubtaskProgressDto> r = taskService.getSubtaskProgressForAssigneeTasks(5L, List.of(10L));

        assertThat(r).hasSize(1);
        assertThat(r.get(0).getParentTaskId()).isEqualTo(10L);
        assertThat(r.get(0).getTotal()).isEqualTo(3L);
        assertThat(r.get(0).getCompleted()).isEqualTo(1L);
    }

    @Test
    void getProjectActivityForAssignee_mergesAndSorts() {
        LocalDateTime tNew = LocalDateTime.of(2026, 2, 10, 0, 0);
        LocalDateTime tOld = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(taskRepository.findMaxTaskUpdatedByProjectForAssignee(7L))
                .thenReturn(List.of(new Object[] {1L, tOld}, new Object[] {2L, tNew}));
        when(subtaskRepository.findMaxSubtaskUpdatedByProjectForAssignee(7L))
                .thenReturn(Collections.singletonList(new Object[] {1L, tNew}));
        when(taskRepository.countOpenTasksByProjectForAssignee(7L))
                .thenReturn(List.of(new Object[] {1L, 2L}, new Object[] {2L, 1L}));

        List<ProjectActivityDto> r = taskService.getProjectActivityForAssignee(7L);

        assertThat(r).hasSize(2);
        assertThat(r.get(0).getProjectId()).isEqualTo(1L);
        assertThat(r.get(0).getLastActivityAt()).isEqualTo(tNew);
        assertThat(r.get(0).getOpenTaskCount()).isEqualTo(2L);
        assertThat(r.get(1).getProjectId()).isEqualTo(2L);
    }

    @Test
    void getProjectActivityForAssignee_whenNullAssignee_returnsEmpty() {
        assertThat(taskService.getProjectActivityForAssignee(null)).isEmpty();
    }

    @Test
    void getSubtaskProgressForAssigneeTasks_whenAssigneeNull_returnsEmpty() {
        assertThat(taskService.getSubtaskProgressForAssigneeTasks(null, List.of(1L))).isEmpty();
        verify(subtaskRepository, never()).aggregateSubtaskProgressByParent(anyLong(), anyList());
    }

    @Test
    void getSubtaskProgressForAssigneeTasks_whenNullTaskIds_returnsEmpty() {
        assertThat(taskService.getSubtaskProgressForAssigneeTasks(1L, null)).isEmpty();
        verify(subtaskRepository, never()).aggregateSubtaskProgressByParent(anyLong(), anyList());
    }

    @Test
    void getSubtaskProgressForAssigneeTasks_whenOnlyNullIds_returnsEmpty() {
        assertThat(taskService.getSubtaskProgressForAssigneeTasks(1L, Arrays.asList(null, null))).isEmpty();
        verify(subtaskRepository, never()).aggregateSubtaskProgressByParent(anyLong(), anyList());
    }

    @Test
    void getSubtaskProgressForAssigneeTasks_deduplicatesBeforeRepository() {
        when(subtaskRepository.aggregateSubtaskProgressByParent(eq(1L), anyList())).thenReturn(Collections.emptyList());

        taskService.getSubtaskProgressForAssigneeTasks(1L, List.of(9L, 9L, 2L));

        verify(subtaskRepository).aggregateSubtaskProgressByParent(eq(1L), eq(List.of(9L, 2L)));
    }

    @Test
    void getSubtaskProgressForAssigneeTasks_limitsTo100DistinctIds() {
        List<Long> ids = LongStream.rangeClosed(1, 150).boxed().toList();
        when(subtaskRepository.aggregateSubtaskProgressByParent(eq(1L), anyList())).thenReturn(Collections.emptyList());

        taskService.getSubtaskProgressForAssigneeTasks(1L, ids);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(subtaskRepository).aggregateSubtaskProgressByParent(eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(100);
        assertThat(captor.getValue().get(0)).isEqualTo(1L);
        assertThat(captor.getValue().get(99)).isEqualTo(100L);
    }

    @Test
    void getProjectActivityForAssignee_whenOnlyOpenCounts_stillReturnsRow() {
        when(taskRepository.findMaxTaskUpdatedByProjectForAssignee(9L)).thenReturn(Collections.emptyList());
        when(subtaskRepository.findMaxSubtaskUpdatedByProjectForAssignee(9L)).thenReturn(Collections.emptyList());
        when(taskRepository.countOpenTasksByProjectForAssignee(9L))
                .thenReturn(Collections.singletonList(new Object[] { 5L, 3L }));

        List<ProjectActivityDto> r = taskService.getProjectActivityForAssignee(9L);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).getProjectId()).isEqualTo(5L);
        assertThat(r.get(0).getOpenTaskCount()).isEqualTo(3L);
        assertThat(r.get(0).getLastActivityAt()).isNull();
    }
}
