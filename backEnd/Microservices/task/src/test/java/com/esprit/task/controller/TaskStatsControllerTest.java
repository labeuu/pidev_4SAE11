package com.esprit.task.controller;

import com.esprit.task.dto.TaskStatsDto;
import com.esprit.task.dto.TaskStatsExtendedDto;
import com.esprit.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskStatsController.class)
class TaskStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void getByProject_returnsStats() throws Exception {
        TaskStatsDto dto = TaskStatsDto.builder()
                .totalTasks(10)
                .doneCount(5)
                .inProgressCount(3)
                .overdueCount(1)
                .completionPercentage(50.0)
                .build();
        when(taskService.getStatsByProject(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/tasks/stats/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(10))
                .andExpect(jsonPath("$.doneCount").value(5));
    }

    @Test
    void getByFreelancer_returnsStats() throws Exception {
        TaskStatsDto dto = TaskStatsDto.builder().totalTasks(5).doneCount(2).build();
        when(taskService.getStatsByFreelancer(eq(10L), any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/tasks/stats/freelancer/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getDashboard_returnsStats() throws Exception {
        TaskStatsDto dto = TaskStatsDto.builder().totalTasks(100).doneCount(40).build();
        when(taskService.getDashboardStats()).thenReturn(dto);

        mockMvc.perform(get("/api/tasks/stats/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void getExtendedByProject_returnsDto() throws Exception {
        TaskStatsExtendedDto dto = TaskStatsExtendedDto.builder()
                .totalTasks(3)
                .doneCount(1)
                .inProgressCount(1)
                .inReviewCount(0)
                .todoCount(1)
                .cancelledCount(0)
                .overdueCount(0)
                .completionPercentage(33.3)
                .unassignedCount(0)
                .build();
        when(taskService.getExtendedStatsByProject(2L)).thenReturn(dto);

        mockMvc.perform(get("/api/tasks/stats/extended/project/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(3))
                .andExpect(jsonPath("$.todoCount").value(1));
    }

    @Test
    void getExtendedDashboard_returnsDto() throws Exception {
        TaskStatsExtendedDto dto = TaskStatsExtendedDto.builder().totalTasks(10).doneCount(5).build();
        when(taskService.getExtendedStatsDashboard()).thenReturn(dto);

        mockMvc.perform(get("/api/tasks/stats/extended/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(10));
    }

    @Test
    void getExtendedByFreelancer_callsServiceWithDueAndActivityRange() throws Exception {
        TaskStatsExtendedDto dto = TaskStatsExtendedDto.builder().totalTasks(1).doneCount(0).build();
        when(taskService.getExtendedStatsByFreelancer(
                eq(7L), any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/tasks/stats/extended/freelancer/7")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30"))
                .andExpect(status().isOk());
    }
}
