package com.esprit.task.service;

import com.esprit.task.client.PlanningClient;
import com.esprit.task.dto.planning.PlanningProgressUpdateCreateDto;
import com.esprit.task.dto.planning.PlanningProgressUpdateDto;
import com.esprit.task.dto.planning.PlanningProgressUpdateRefDto;
import com.esprit.task.entity.Subtask;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskStatusProgressBridgeTest {

    @Mock
    private PlanningClient planningClient;

    @InjectMocks
    private TaskStatusProgressBridge bridge;

    @Test
    void resolveProgressPercentage_matchesHeuristics() {
        assertThat(TaskStatusProgressBridge.resolveProgressPercentage(0, TaskStatus.TODO)).isEqualTo(25);
        assertThat(TaskStatusProgressBridge.resolveProgressPercentage(0, TaskStatus.IN_PROGRESS)).isEqualTo(55);
        assertThat(TaskStatusProgressBridge.resolveProgressPercentage(0, TaskStatus.IN_REVIEW)).isEqualTo(85);
        assertThat(TaskStatusProgressBridge.resolveProgressPercentage(0, TaskStatus.DONE)).isEqualTo(100);
        assertThat(TaskStatusProgressBridge.resolveProgressPercentage(72, TaskStatus.CANCELLED)).isEqualTo(72);
    }

    @Test
    void afterRootTaskSkipped_whenFlagFalse() {
        ReflectionTestUtils.setField(bridge, "autoProgressOnStatusChange", false);
        Task t = rootTask(TaskStatus.IN_PROGRESS);
        bridge.afterRootTaskStatusChanged(t);
        verify(planningClient, never()).listProgressUpdatesByProject(anyLong());
    }

    @Test
    void afterRootTaskSkipped_whenNoAssignee() {
        ReflectionTestUtils.setField(bridge, "autoProgressOnStatusChange", true);
        Task t = rootTask(TaskStatus.IN_PROGRESS);
        t.setAssigneeId(null);
        bridge.afterRootTaskStatusChanged(t);
        verify(planningClient, never()).listProgressUpdatesByProject(anyLong());
    }

    @Test
    void afterRootTask_createsWithMaxOfHeuristicAndExisting() {
        ReflectionTestUtils.setField(bridge, "autoProgressOnStatusChange", true);
        when(planningClient.listProgressUpdatesByProject(1L))
                .thenReturn(List.of(PlanningProgressUpdateDto.builder().progressPercentage(80).build()));
        when(planningClient.createProgressUpdate(any())).thenReturn(new PlanningProgressUpdateRefDto());

        Task t = rootTask(TaskStatus.DONE);
        bridge.afterRootTaskStatusChanged(t);

        ArgumentCaptor<PlanningProgressUpdateCreateDto> cap = ArgumentCaptor.forClass(PlanningProgressUpdateCreateDto.class);
        verify(planningClient).createProgressUpdate(cap.capture());
        assertThat(cap.getValue().getProgressPercentage()).isEqualTo(100);
        assertThat(cap.getValue().getFreelancerId()).isEqualTo(9L);
        assertThat(cap.getValue().getProjectId()).isEqualTo(1L);
    }

    @Test
    void afterRootTask_skipsWhenListFeignFails() {
        ReflectionTestUtils.setField(bridge, "autoProgressOnStatusChange", true);
        Request req = Request.create(
                Request.HttpMethod.GET,
                "/x",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8);
        Response resp = Response.builder().status(500).request(req).build();
        when(planningClient.listProgressUpdatesByProject(1L))
                .thenThrow(FeignException.errorStatus("PlanningClient#listProgressUpdatesByProject", resp));

        bridge.afterRootTaskStatusChanged(rootTask(TaskStatus.DONE));

        verify(planningClient, never()).createProgressUpdate(any());
    }

    @Test
    void afterRootTask_skipsCreateFailureWithoutThrowing() {
        ReflectionTestUtils.setField(bridge, "autoProgressOnStatusChange", true);
        when(planningClient.listProgressUpdatesByProject(1L)).thenReturn(List.of());
        Request req = Request.create(
                Request.HttpMethod.POST,
                "/x",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8);
        Response resp = Response.builder().status(400).request(req).build();
        when(planningClient.createProgressUpdate(any()))
                .thenThrow(FeignException.errorStatus("PlanningClient#createProgressUpdate", resp));

        bridge.afterRootTaskStatusChanged(rootTask(TaskStatus.IN_PROGRESS));
    }

    @Test
    void afterSubtask_usesParentContractId() {
        ReflectionTestUtils.setField(bridge, "autoProgressOnStatusChange", true);
        Task parent = rootTask(TaskStatus.TODO);
        parent.setContractId(500L);
        when(planningClient.listProgressUpdatesByProject(1L)).thenReturn(List.of());
        when(planningClient.createProgressUpdate(any())).thenReturn(new PlanningProgressUpdateRefDto());

        Subtask s = Subtask.builder()
                .id(3L)
                .parent(parent)
                .projectId(1L)
                .title("Sub")
                .status(TaskStatus.IN_REVIEW)
                .priority(TaskPriority.MEDIUM)
                .assigneeId(9L)
                .orderIndex(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        bridge.afterSubtaskStatusChanged(s);

        ArgumentCaptor<PlanningProgressUpdateCreateDto> cap = ArgumentCaptor.forClass(PlanningProgressUpdateCreateDto.class);
        verify(planningClient).createProgressUpdate(cap.capture());
        assertThat(cap.getValue().getContractId()).isEqualTo(500L);
        assertThat(cap.getValue().getProgressPercentage()).isEqualTo(85);
    }

    private static Task rootTask(TaskStatus status) {
        Task t = new Task();
        t.setId(2L);
        t.setProjectId(1L);
        t.setContractId(33L);
        t.setTitle("Root task");
        t.setStatus(status);
        t.setPriority(TaskPriority.MEDIUM);
        t.setAssigneeId(9L);
        t.setOrderIndex(0);
        return t;
    }
}
