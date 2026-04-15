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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubtaskServiceTest {

    @Mock
    private SubtaskRepository subtaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskNotificationService taskNotificationService;

    @Mock
    private TaskStatusProgressBridge taskStatusProgressBridge;

    @InjectMocks
    private SubtaskService subtaskService;

    private static Task parent(long id) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(7L);
        t.setTitle("Root");
        t.setStatus(TaskStatus.TODO);
        t.setPriority(TaskPriority.MEDIUM);
        t.setOrderIndex(0);
        t.setAssigneeId(99L);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    void listByParentTaskId_whenParentMissing_throws() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtaskService.listByParentTaskId(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listByParentTaskId_returnsMappedRows() {
        Task p = parent(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(p));
        Subtask s = Subtask.builder()
                .id(2L)
                .parent(p)
                .projectId(7L)
                .title("A")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .assigneeId(99L)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findByParent_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(s));

        List<SubtaskResponse> rows = subtaskService.listByParentTaskId(1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getTitle()).isEqualTo("A");
        assertThat(rows.get(0).getParentTaskId()).isEqualTo(1L);
    }

    @Test
    void create_persistsWithProjectFromParent() {
        Task p = parent(1L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(p));
        when(subtaskRepository.findMaxOrderIndexByParent(1L)).thenReturn(0);
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(inv -> {
            Subtask x = inv.getArgument(0);
            x.setId(50L);
            x.setCreatedAt(LocalDateTime.now());
            x.setUpdatedAt(LocalDateTime.now());
            return x;
        });

        SubtaskRequest req = SubtaskRequest.builder()
                .title("  New sub  ")
                .description("d")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDate.of(2026, 6, 1))
                .build();

        SubtaskResponse res = subtaskService.create(1L, req);

        assertThat(res.getId()).isEqualTo(50L);
        assertThat(res.getProjectId()).isEqualTo(7L);
        ArgumentCaptor<Subtask> cap = ArgumentCaptor.forClass(Subtask.class);
        verify(subtaskRepository).save(cap.capture());
        assertThat(cap.getValue().getTitle()).isEqualTo("New sub");
        assertThat(cap.getValue().getProjectId()).isEqualTo(7L);
    }

    @Test
    void patchStatus_notifiesWhenChanged() {
        Subtask s = Subtask.builder()
                .id(3L)
                .parent(parent(1L))
                .projectId(7L)
                .title("x")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .assigneeId(100L)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findById(3L)).thenReturn(Optional.of(s));
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(inv -> inv.getArgument(0));

        subtaskService.patchStatus(3L, TaskStatus.DONE);

        verify(taskNotificationService).notifySubtaskStatusUpdate(any(Subtask.class));
        verify(taskStatusProgressBridge).afterSubtaskStatusChanged(any(Subtask.class));
    }

    @Test
    void findEntityById_returnsEntity() {
        Subtask s = Subtask.builder()
                .id(4L)
                .parent(parent(1L))
                .projectId(7L)
                .title("e")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findById(4L)).thenReturn(Optional.of(s));

        assertThat(subtaskService.findEntityById(4L).getId()).isEqualTo(4L);
    }

    @Test
    void findEntityById_whenMissing_throws() {
        when(subtaskRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtaskService.findEntityById(4L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_whenStatusUnchanged_doesNotNotify() {
        Subtask s = Subtask.builder()
                .id(3L)
                .parent(parent(1L))
                .projectId(7L)
                .title("x")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findById(3L)).thenReturn(Optional.of(s));
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtaskRequest req = SubtaskRequest.builder()
                .title("  t2  ")
                .status(TaskStatus.TODO)
                .build();
        subtaskService.update(3L, req);

        verify(taskNotificationService, never()).notifySubtaskStatusUpdate(any());
        verify(taskStatusProgressBridge, never()).afterSubtaskStatusChanged(any());
    }

    @Test
    void update_whenStatusChanges_notifies() {
        Subtask s = Subtask.builder()
                .id(3L)
                .parent(parent(1L))
                .projectId(7L)
                .title("x")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .assigneeId(101L)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findById(3L)).thenReturn(Optional.of(s));
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtaskRequest req = SubtaskRequest.builder().status(TaskStatus.DONE).build();
        subtaskService.update(3L, req);

        verify(taskNotificationService).notifySubtaskStatusUpdate(any(Subtask.class));
        verify(taskStatusProgressBridge).afterSubtaskStatusChanged(any(Subtask.class));
    }

    @Test
    void patchDueDate_updatesField() {
        Subtask s = Subtask.builder()
                .id(3L)
                .parent(parent(1L))
                .projectId(7L)
                .title("x")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findById(3L)).thenReturn(Optional.of(s));
        LocalDate d = LocalDate.of(2026, 8, 1);
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(inv -> inv.getArgument(0));

        SubtaskResponse r = subtaskService.patchDueDate(3L, d);

        assertThat(r.getDueDate()).isEqualTo(d);
    }

    @Test
    void deleteById_removes() {
        Subtask s = Subtask.builder()
                .id(8L)
                .parent(parent(1L))
                .projectId(7L)
                .title("x")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findById(8L)).thenReturn(Optional.of(s));

        subtaskService.deleteById(8L);

        verify(subtaskRepository).delete(s);
    }

    @Test
    void deleteById_whenMissing_throws() {
        when(subtaskRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtaskService.deleteById(8L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void patchStatus_whenUnchanged_doesNotNotify() {
        Subtask s = Subtask.builder()
                .id(3L)
                .parent(parent(1L))
                .projectId(7L)
                .title("x")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(subtaskRepository.findById(3L)).thenReturn(Optional.of(s));
        when(subtaskRepository.save(any(Subtask.class))).thenAnswer(inv -> inv.getArgument(0));

        subtaskService.patchStatus(3L, TaskStatus.TODO);

        verify(taskNotificationService, never()).notifySubtaskStatusUpdate(any());
        verify(taskStatusProgressBridge, never()).afterSubtaskStatusChanged(any());
    }
}
