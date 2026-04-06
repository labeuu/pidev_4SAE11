package com.esprit.task.service;

import com.esprit.task.client.NotificationClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskNotificationService. Verifies notifyTaskStatusUpdate
 * fetches project, builds correct notification payload, and calls NotificationClient.
 */
@ExtendWith(MockitoExtension.class)
class TaskNotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private ProjectClient projectClient;

    @InjectMocks
    private TaskNotificationService taskNotificationService;

    private static Task task(Long id) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(1L);
        t.setTitle("Test Task");
        t.setStatus(TaskStatus.IN_PROGRESS);
        return t;
    }

    @Test
    void notifyTaskStatusUpdate_callsClientWithCorrectPayload() {
        Task t = task(1L);
        ProjectDto project = new ProjectDto(1L, 100L, "Project A", null, null);
        when(projectClient.getProjectById(1L)).thenReturn(project);

        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient).create(captor.capture());
        var dto = captor.getValue();
        assertThat(dto.getUserId()).isEqualTo("100");
        assertThat(dto.getTitle()).isEqualTo("Task status updated");
        assertThat(dto.getBody()).contains("Test Task").contains("IN PROGRESS");
        assertThat(dto.getType()).isEqualTo(TaskNotificationService.TYPE_TASK_STATUS_UPDATE);
        assertThat(dto.getData()).containsEntry("projectId", "1").containsEntry("taskId", "1");
    }

    @Test
    void notifyTaskStatusUpdate_whenTaskNull_doesNotCallClient() {
        taskNotificationService.notifyTaskStatusUpdate(null);
        verify(notificationClient, never()).create(any());
        verify(projectClient, never()).getProjectById(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectIdNull_doesNotCallClient() {
        Task t = task(1L);
        t.setProjectId(null);
        taskNotificationService.notifyTaskStatusUpdate(t);
        verify(notificationClient, never()).create(any());
        verify(projectClient, never()).getProjectById(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectNotFound_doesNotCallNotification() {
        Task t = task(1L);
        when(projectClient.getProjectById(1L)).thenThrow(new RuntimeException("Not found"));

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectNull_doesNotCallNotification() {
        Task t = task(1L);
        when(projectClient.getProjectById(1L)).thenReturn(null);

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenProjectClientIdNull_doesNotCallNotification() {
        Task t = task(1L);
        ProjectDto project = new ProjectDto(1L, null, "Project A", null, null);
        when(projectClient.getProjectById(1L)).thenReturn(project);

        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenTaskTitleNull_usesFallback() {
        Task t = task(1L);
        t.setTitle(null);
        ProjectDto project = new ProjectDto(1L, 100L, "Project A", null, null);
        when(projectClient.getProjectById(1L)).thenReturn(project);

        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);
        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient).create(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Task #1");
    }

    @Test
    void notifyTaskPriorityEscalated_callsClientForAssignee() {
        Task t = task(1L);
        t.setAssigneeId(42L);
        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);

        taskNotificationService.notifyTaskPriorityEscalated(t);

        verify(notificationClient).create(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("42");
        assertThat(captor.getValue().getType()).isEqualTo(TaskNotificationService.TYPE_TASK_PRIORITY_ESCALATED);
        assertThat(captor.getValue().getData()).containsEntry("projectId", "1").containsEntry("taskId", "1");
        verify(projectClient, never()).getProjectById(any());
    }

    @Test
    void notifyTaskPriorityEscalated_whenAssigneeNull_doesNotCallClient() {
        Task t = task(1L);
        t.setAssigneeId(null);
        taskNotificationService.notifyTaskPriorityEscalated(t);
        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskPriorityEscalated_whenTaskNull_doesNotCallClient() {
        taskNotificationService.notifyTaskPriorityEscalated(null);
        verify(notificationClient, never()).create(any());
    }

    @Test
    void notifyTaskStatusUpdate_whenStatusNull_usesUpdatedInBody() {
        Task t = task(1L);
        t.setStatus(null);
        ProjectDto project = new ProjectDto(1L, 100L, "Project A", null, null);
        when(projectClient.getProjectById(1L)).thenReturn(project);

        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);
        taskNotificationService.notifyTaskStatusUpdate(t);

        verify(notificationClient).create(captor.capture());
        assertThat(captor.getValue().getBody()).contains("updated");
    }

    @Test
    void notifyTaskPriorityEscalated_whenTitleNull_usesFallbackInBody() {
        Task t = task(5L);
        t.setTitle(null);
        t.setAssigneeId(9L);
        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);

        taskNotificationService.notifyTaskPriorityEscalated(t);

        verify(notificationClient).create(captor.capture());
        assertThat(captor.getValue().getBody()).contains("Task #5");
    }

    @Test
    void notifyTaskPriorityEscalated_whenNotificationClientThrows_doesNotPropagate() {
        Task t = task(1L);
        t.setAssigneeId(7L);
        when(notificationClient.create(any())).thenThrow(new RuntimeException("down"));

        assertThatCode(() -> taskNotificationService.notifyTaskPriorityEscalated(t))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyTaskStatusUpdate_whenNotificationClientThrows_doesNotPropagate() {
        Task t = task(1L);
        ProjectDto project = new ProjectDto(1L, 100L, "Project A", null, null);
        when(projectClient.getProjectById(1L)).thenReturn(project);
        when(notificationClient.create(any())).thenThrow(new RuntimeException("Service unavailable"));

        assertThatCode(() -> taskNotificationService.notifyTaskStatusUpdate(t))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyFreelancerDailyOverdueReminder_callsClientWithDigest() {
        ArgumentCaptor<com.esprit.task.dto.NotificationRequestDto> captor =
                ArgumentCaptor.forClass(com.esprit.task.dto.NotificationRequestDto.class);
        taskNotificationService.notifyFreelancerDailyOverdueReminder(15L,
                List.of("Task: A — due 2026-01-01", "Subtask: B — due 2026-01-02"));

        verify(notificationClient).create(captor.capture());
        var dto = captor.getValue();
        assertThat(dto.getUserId()).isEqualTo("15");
        assertThat(dto.getType()).isEqualTo(TaskNotificationService.TYPE_TASK_OVERDUE_DAILY_REMINDER);
        assertThat(dto.getBody()).contains("2 overdue").contains("Task: A").contains("Subtask: B");
        assertThat(dto.getData()).containsEntry("overdueCount", "2").containsEntry("reminderKind", "OVERDUE_DAILY");
        verify(projectClient, never()).getProjectById(any());
    }

    @Test
    void notifyFreelancerDailyOverdueReminder_whenEmpty_doesNotCallClient() {
        taskNotificationService.notifyFreelancerDailyOverdueReminder(1L, List.of());
        taskNotificationService.notifyFreelancerDailyOverdueReminder(1L, null);
        taskNotificationService.notifyFreelancerDailyOverdueReminder(null, List.of("x"));
        verify(notificationClient, never()).create(any());
    }
}
