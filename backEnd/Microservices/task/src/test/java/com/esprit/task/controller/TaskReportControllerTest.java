package com.esprit.task.controller;

import com.esprit.task.service.TaskWeeklyReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskReportController.class)
class TaskReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskWeeklyReportService taskWeeklyReportService;

    @Test
    void weeklyPdf_returnsAttachment() throws Exception {
        byte[] fakePdf = new byte[] {'%', 'P', 'D', 'F', '-', 1, 2, 3};
        when(taskWeeklyReportService.buildWeeklyPdf(any(), any(), any())).thenReturn(fakePdf);

        MvcResult result = mockMvc.perform(get("/api/tasks/reports/weekly.pdf"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentType()).contains("pdf");
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("attachment");
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("task-report-week-");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(fakePdf);
    }

    @Test
    void weeklyPdf_withWeekStart_setsFilenameFromNormalizedMonday() throws Exception {
        when(taskWeeklyReportService.buildWeeklyPdf(any(), any(), any())).thenReturn(new byte[] {'%', 'P', 'D', 'F'});

        LocalDate wednesday = LocalDate.of(2026, 4, 8);
        LocalDate monday = TaskWeeklyReportService.normalizeWeekStartMonday(wednesday);

        MvcResult result = mockMvc.perform(get("/api/tasks/reports/weekly.pdf").param("weekStart", wednesday.toString()))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition")).contains(monday.toString());
    }

    @Test
    void weeklyPdf_withLastDays_usesRollingWindowFilename() throws Exception {
        byte[] fakePdf = new byte[] {'%', 'P', 'D', 'F'};
        when(taskWeeklyReportService.buildRollingPeriodPdf(any(), any(), eq(LocalDate.of(2026, 4, 10)), eq(7)))
                .thenReturn(fakePdf);

        MvcResult result = mockMvc.perform(get("/api/tasks/reports/weekly.pdf")
                        .param("lastDays", "7")
                        .param("periodEnd", "2026-04-10")
                        .param("freelancerId", "5"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("2026-04-04-to-2026-04-10");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(fakePdf);
    }
}
