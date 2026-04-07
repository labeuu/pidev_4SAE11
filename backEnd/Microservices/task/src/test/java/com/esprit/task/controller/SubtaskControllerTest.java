package com.esprit.task.controller;

import com.esprit.task.config.GlobalExceptionHandler;
import com.esprit.task.dto.SubtaskResponse;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.service.SubtaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubtaskController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "welcome.message=Welcome to Task API")
class SubtaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubtaskService subtaskService;

    private static SubtaskResponse response(long id) {
        return response(id, "Sub", TaskStatus.TODO);
    }

    private static SubtaskResponse response(long id, String title, TaskStatus status) {
        return SubtaskResponse.builder()
                .id(id)
                .parentTaskId(10L)
                .projectId(1L)
                .title(title)
                .description(null)
                .status(status)
                .priority(TaskPriority.MEDIUM)
                .assigneeId(5L)
                .dueDate(LocalDate.now().plusDays(1))
                .orderIndex(0)
                .build();
    }

    @Test
    void patchStatus_returns200() throws Exception {
        when(subtaskService.patchStatus(eq(3L), eq(TaskStatus.DONE))).thenReturn(response(3L, "Sub", TaskStatus.DONE));

        mockMvc.perform(patch("/api/subtasks/3/status").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("DONE"));

        verify(subtaskService).patchStatus(3L, TaskStatus.DONE);
    }

    @Test
    void patchDueDate_returns200() throws Exception {
        when(subtaskService.patchDueDate(eq(3L), any(LocalDate.class))).thenReturn(response(3L));

        mockMvc.perform(patch("/api/subtasks/3/due-date").param("dueDate", "2026-05-01"))
                .andExpect(status().isOk());

        verify(subtaskService).patchDueDate(eq(3L), any(LocalDate.class));
    }

    @Test
    void update_returns200() throws Exception {
        when(subtaskService.update(eq(3L), any())).thenReturn(response(3L, "Updated", TaskStatus.IN_PROGRESS));

        String body = """
                {"title":"Updated","description":null,"status":"IN_PROGRESS","priority":"HIGH","assigneeId":5,"dueDate":null,"orderIndex":1}
                """;
        mockMvc.perform(put("/api/subtasks/3").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        verify(subtaskService).update(eq(3L), any());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/subtasks/3"))
                .andExpect(status().isNoContent());

        verify(subtaskService).deleteById(3L);
    }
}
