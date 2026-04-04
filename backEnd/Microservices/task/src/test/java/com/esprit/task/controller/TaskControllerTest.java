package com.esprit.task.controller;

import com.esprit.task.dto.ProjectActivityDto;
import com.esprit.task.dto.SubtaskProgressDto;
import com.esprit.task.dto.TaskCalendarEventDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.service.SubtaskService;
import com.esprit.task.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.esprit.task.config.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "welcome.message=Welcome to Task API")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private SubtaskService subtaskService;

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
    void getFiltered_returnsPaginatedResults() throws Exception {
        Task t = task(1L);
        Page<Task> page = new PageImpl<>(List.of(t), PageRequest.of(0, 20), 1);
        when(taskService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/tasks").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getFiltered_capsPageSizeAt100() throws Exception {
        when(taskService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        mockMvc.perform(get("/api/tasks").param("size", "500"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), cap.capture());
        assertThat(cap.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getById_returnsTask() throws Exception {
        Task t = task(1L);
        when(taskService.findById(1L)).thenReturn(t);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Task 1"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(taskService.findById(999L)).thenThrow(new EntityNotFoundException("Task", 999L));

        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getByProjectId_returnsList() throws Exception {
        Task t = task(1L);
        when(taskService.findByProjectId(1L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getBoardByProject_returnsBoard() throws Exception {
        when(taskService.getBoardByProject(1L)).thenReturn(
                com.esprit.task.dto.TaskBoardDto.builder().projectId(1L).columns(Map.of()).build());

        mockMvc.perform(get("/api/tasks/board/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1));
    }

    @Test
    void getOverdue_returnsList() throws Exception {
        when(taskService.getOverdueTasks(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getDueSoon_invokesService() throws Exception {
        when(taskService.findDueSoon(any(), any(), eq(14))).thenReturn(List.of(task(1L)));

        mockMvc.perform(get("/api/tasks/due-soon").param("withinDays", "14").param("projectId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(taskService).findDueSoon(eq(Optional.of(2L)), eq(Optional.empty()), eq(14));
    }

    @Test
    void getRootTasksByProject_returnsList() throws Exception {
        when(taskService.findRootTasksByProject(5L)).thenReturn(List.of(task(1L)));

        mockMvc.perform(get("/api/tasks/project/5/root-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
        verify(taskService).findRootTasksByProject(5L);
    }

    @Test
    void getCalendarEvents_returnsEvents() throws Exception {
        TaskCalendarEventDto evt = TaskCalendarEventDto.builder()
                .id("task-1")
                .summary("Task deadline – Test")
                .start(LocalDateTime.now())
                .end(LocalDateTime.now().plusHours(1))
                .build();
        when(taskService.getCalendarEvents(any(), any(), any())).thenReturn(List.of(evt));

        mockMvc.perform(get("/api/tasks/calendar-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("task-1"));
    }

    @Test
    void create_returns201() throws Exception {
        Task t = task(1L);
        when(taskService.create(any())).thenReturn(t);

        String body = """
                {"projectId":1,"title":"New Task","status":"TODO","priority":"MEDIUM"}
                """;
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_whenTitleMissing_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON).content("{\"projectId\":1,\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void create_whenProjectIdMissing_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON).content("{\"title\":\"Only title\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.projectId").exists());
    }

    @Test
    void bulkPatchStatus_returns200() throws Exception {
        Task t = task(1L);
        t.setStatus(TaskStatus.DONE);
        when(taskService.bulkPatchStatus(any(), eq(TaskStatus.DONE))).thenReturn(List.of(t));

        String body = """
                {"taskIds":[1,2],"status":"DONE"}
                """;
        mockMvc.perform(post("/api/tasks/bulk/status").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DONE"));
        verify(taskService).bulkPatchStatus(eq(List.of(1L, 2L)), eq(TaskStatus.DONE));
    }

    @Test
    void patchDueDate_returns200() throws Exception {
        Task t = task(1L);
        t.setDueDate(LocalDate.of(2026, 5, 1));
        when(taskService.patchDueDate(1L, LocalDate.of(2026, 5, 1))).thenReturn(t);

        mockMvc.perform(patch("/api/tasks/1/due-date").param("dueDate", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2026-05-01"));
    }

    @Test
    void update_returns200() throws Exception {
        Task t = task(1L);
        when(taskService.update(eq(1L), any())).thenReturn(t);

        String body = """
                {"projectId":1,"title":"Updated","status":"IN_PROGRESS","priority":"HIGH"}
                """;
        mockMvc.perform(put("/api/tasks/1").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void patchStatus_returns200() throws Exception {
        Task t = task(1L);
        t.setStatus(TaskStatus.IN_PROGRESS);
        when(taskService.patchStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(t);

        mockMvc.perform(patch("/api/tasks/1/status").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());
        verify(taskService).deleteById(1L);
    }

    @Test
    void getByContractId_returnsList() throws Exception {
        Task t = task(1L);
        when(taskService.findByContractId(1L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks/contract/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getByAssigneeId_returnsList() throws Exception {
        Task t = task(1L);
        when(taskService.findByAssigneeId(10L)).thenReturn(List.of(t));

        mockMvc.perform(get("/api/tasks/assignee/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getSubtaskProgress_returnsDtos() throws Exception {
        when(taskService.getSubtaskProgressForAssigneeTasks(eq(10L), eq(List.of(1L, 2L))))
                .thenReturn(List.of(
                        SubtaskProgressDto.builder().parentTaskId(1L).total(4).completed(2).build()));

        mockMvc.perform(get("/api/tasks/assignee/10/subtask-progress").param("taskIds", "1, 2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parentTaskId").value(1))
                .andExpect(jsonPath("$[0].total").value(4))
                .andExpect(jsonPath("$[0].completed").value(2));
    }

    @Test
    void getSubtaskProgress_whenTaskIdsMissing_returnsEmptyFromService() throws Exception {
        when(taskService.getSubtaskProgressForAssigneeTasks(eq(10L), eq(List.of()))).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks/assignee/10/subtask-progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getProjectActivity_returnsSortedList() throws Exception {
        LocalDateTime recent = LocalDateTime.of(2026, 3, 1, 12, 0);
        when(taskService.getProjectActivityForAssignee(10L)).thenReturn(List.of(
                ProjectActivityDto.builder().projectId(2L).lastActivityAt(recent).openTaskCount(3).build(),
                ProjectActivityDto.builder().projectId(1L).lastActivityAt(recent.minusDays(1)).openTaskCount(1).build()));

        mockMvc.perform(get("/api/tasks/assignee/10/project-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(2))
                .andExpect(jsonPath("$[1].projectId").value(1));
    }

    @Test
    void getSubtaskProgress_parsesTaskIds_skipsInvalidAndEmptyTokens() throws Exception {
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        when(taskService.getSubtaskProgressForAssigneeTasks(eq(10L), anyList())).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks/assignee/10/subtask-progress")
                        .param("taskIds", "1, x , 3,,5"))
                .andExpect(status().isOk());

        verify(taskService).getSubtaskProgressForAssigneeTasks(eq(10L), captor.capture());
        assertThat(captor.getValue()).containsExactly(1L, 3L, 5L);
    }

    @Test
    void getSubtaskProgress_capsParsedIdsAt100() throws Exception {
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        when(taskService.getSubtaskProgressForAssigneeTasks(eq(10L), anyList())).thenReturn(List.of());
        String ids = IntStream.rangeClosed(1, 101)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        mockMvc.perform(get("/api/tasks/assignee/10/subtask-progress").param("taskIds", ids))
                .andExpect(status().isOk());

        verify(taskService).getSubtaskProgressForAssigneeTasks(eq(10L), captor.capture());
        assertThat(captor.getValue()).hasSize(100);
        assertThat(captor.getValue().get(0)).isEqualTo(1L);
        assertThat(captor.getValue().get(99)).isEqualTo(100L);
    }

    @Test
    void getProjectActivity_serializesOpenCountAndLastActivity() throws Exception {
        LocalDateTime t = LocalDateTime.of(2026, 4, 1, 9, 30);
        when(taskService.getProjectActivityForAssignee(10L)).thenReturn(List.of(
                ProjectActivityDto.builder().projectId(7L).lastActivityAt(t).openTaskCount(12L).build()));

        mockMvc.perform(get("/api/tasks/assignee/10/project-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].openTaskCount").value(12))
                .andExpect(jsonPath("$[0].lastActivityAt").exists());
    }

    @Test
    void patchAssignee_returns200() throws Exception {
        Task t = task(1L);
        when(taskService.patchAssignee(1L, 10L)).thenReturn(t);

        mockMvc.perform(patch("/api/tasks/1/assignee").param("assigneeId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void reorder_returns200() throws Exception {
        mockMvc.perform(post("/api/tasks/reorder")
                        .contentType(APPLICATION_JSON)
                        .content("[1, 2, 3]"))
                .andExpect(status().isOk());
        verify(taskService).reorder(any());
    }

    @Test
    void reorder_whenBodyMissing_returns400WithJsonMessage() throws Exception {
        mockMvc.perform(post("/api/tasks/reorder").contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or missing JSON body"));
    }

    @Test
    void getFiltered_withSortParam_invokesService() throws Exception {
        Page<Task> page = new PageImpl<>(List.of(task(1L)), PageRequest.of(0, 20), 1);
        when(taskService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "dueDate,desc"))
                .andExpect(status().isOk());
    }

    @Test
    void getFiltered_withAllFilters_invokesService() throws Exception {
        Page<Task> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(taskService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/tasks")
                        .param("projectId", "1")
                        .param("contractId", "2")
                        .param("assigneeId", "10")
                        .param("status", "IN_PROGRESS")
                        .param("priority", "HIGH")
                        .param("search", "test")
                        .param("dueDateFrom", "2024-01-01")
                        .param("dueDateTo", "2024-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void getCalendarEvents_withParams_invokesService() throws Exception {
        TaskCalendarEventDto evt = TaskCalendarEventDto.builder()
                .id("task-1")
                .summary("Test")
                .start(LocalDateTime.now())
                .end(LocalDateTime.now().plusHours(1))
                .build();
        when(taskService.getCalendarEvents(any(), any(), any())).thenReturn(List.of(evt));

        mockMvc.perform(get("/api/tasks/calendar-events")
                        .param("timeMin", "2024-01-01T00:00:00Z")
                        .param("timeMax", "2024-12-31T23:59:59Z")
                        .param("userId", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void create_withFullRequest_returns201() throws Exception {
        Task t = task(1L);
        when(taskService.create(any())).thenReturn(t);

        String body = """
                {"projectId":1,"contractId":2,"title":"Task","description":"Desc","status":"TODO","priority":"HIGH","assigneeId":10,"dueDate":"2024-06-15","orderIndex":0}
                """;
        mockMvc.perform(post("/api/tasks").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}
