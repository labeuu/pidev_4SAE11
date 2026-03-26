package com.esprit.planning.controller;

import com.esprit.planning.dto.ProgressSummaryItemDto;
import com.esprit.planning.dto.ProgressTrendPointDto;
import com.esprit.planning.dto.ProgressUpdateRequest;
import com.esprit.planning.dto.ProgressUpdateValidationResponse;
import com.esprit.planning.dto.StalledProjectDto;
import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import com.esprit.planning.service.ProgressCommentService;
import com.esprit.planning.service.ProgressUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProgressUpdateController. Verifies that each API endpoint delegates to the service
 * and returns the expected HTTP status and response body.
 */
@WebMvcTest(ProgressUpdateController.class)
@TestPropertySource(properties = "welcome.message=Welcome to Planning API")
class ProgressUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProgressUpdateService progressUpdateService;

    @MockitoBean
    private ProgressCommentService progressCommentService;

    @Test
    void welcome_returnsWelcomeMessage() throws Exception {
        mockMvc.perform(get("/api/progress-updates/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to Planning API"));
    }

    @Test
    void getFiltered_returnsPaginatedResults() throws Exception {
        ProgressUpdate update = progressUpdate(1L, 1L, 10L, "Title", 50);
        Page<ProgressUpdate> page = new PageImpl<>(List.of(update), PageRequest.of(0, 20), 1);
        when(progressUpdateService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/progress-updates")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Title"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getFiltered_withSortParam_passesSortToService() throws Exception {
        when(progressUpdateService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/progress-updates")
                        .param("sort", "createdAt,asc"))
                .andExpect(status().isOk());
        verify(progressUpdateService).findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getFiltered_withSortSingleField_parsesSortWithDefaultDirection() throws Exception {
        when(progressUpdateService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/progress-updates").param("sort", "title"))
                .andExpect(status().isOk());
        verify(progressUpdateService).findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getFiltered_withFilters_passesParamsToService() throws Exception {
        when(progressUpdateService.findAllFiltered(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/progress-updates")
                        .param("projectId", "1")
                        .param("freelancerId", "10")
                        .param("progressMin", "0")
                        .param("progressMax", "100")
                        .param("dateFrom", "2025-01-01")
                        .param("dateTo", "2025-12-31")
                        .param("search", "test"))
                .andExpect(status().isOk());
        verify(progressUpdateService).findAllFiltered(
                eq(Optional.of(1L)), eq(Optional.of(10L)), eq(Optional.empty()),
                eq(Optional.of(0)), eq(Optional.of(100)),
                eq(Optional.of(LocalDate.of(2025, 1, 1))), eq(Optional.of(LocalDate.of(2025, 12, 31))),
                eq(Optional.of("test")), any());
    }

    @Test
    void export_returnsCsv() throws Exception {
        when(progressUpdateService.findAllFilteredForExport(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(progressUpdate(1L, 1L, 10L, "Title", 75)));

        mockMvc.perform(get("/api/progress-updates/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id,projectId,")));
    }

    @Test
    void export_withSpecialChars_escapesCsv() throws Exception {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Title, with comma", 75);
        when(progressUpdateService.findAllFilteredForExport(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(u));

        mockMvc.perform(get("/api/progress-updates/export"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"Title, with comma\"")));
    }

    @Test
    void getById_returnsProgressUpdate() throws Exception {
        ProgressUpdate update = progressUpdate(1L, 1L, 10L, "Title", 50);
        when(progressUpdateService.findById(1L)).thenReturn(update);

        mockMvc.perform(get("/api/progress-updates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void getByIdWithComments_returnsUpdateAndComments() throws Exception {
        ProgressUpdate update = progressUpdate(1L, 1L, 10L, "Title", 50);
        ProgressComment comment = ProgressComment.builder().id(1L).userId(5L).message("Nice").build();
        when(progressUpdateService.findById(1L)).thenReturn(update);
        when(progressCommentService.findByProgressUpdateId(1L)).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/progress-updates/1/with-comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressUpdate.id").value(1))
                .andExpect(jsonPath("$.comments[0].message").value("Nice"));
    }

    @Test
    void getByProjectId_returnsList() throws Exception {
        when(progressUpdateService.findByProjectId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        mockMvc.perform(get("/api/progress-updates/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1));
    }

    @Test
    void getByContractId_returnsList() throws Exception {
        when(progressUpdateService.findByContractId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        mockMvc.perform(get("/api/progress-updates/contract/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getByFreelancerId_returnsList() throws Exception {
        when(progressUpdateService.findByFreelancerId(10L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        mockMvc.perform(get("/api/progress-updates/freelancer/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].freelancerId").value(10));
    }

    @Test
    void getSummary_withProjectIds_returnsList() throws Exception {
        when(progressUpdateService.getSummaryByProjectIds(anyList())).thenReturn(List.of(
                ProgressSummaryItemDto.builder().projectId(1L).currentProgressPercentage(72).build()));

        mockMvc.perform(get("/api/progress-updates/summary").param("projectIds", "1, 2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1))
                .andExpect(jsonPath("$[0].currentProgressPercentage").value(72));
        verify(progressUpdateService).getSummaryByProjectIds(List.of(1L, 2L));
    }

    @Test
    void getSummary_withContractIds_returnsList() throws Exception {
        when(progressUpdateService.getSummaryByContractIds(anyList())).thenReturn(List.of(
                ProgressSummaryItemDto.builder().contractId(5L).projectId(9L).currentProgressPercentage(40).build()));

        mockMvc.perform(get("/api/progress-updates/summary").param("contractIds", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contractId").value(5));
        verify(progressUpdateService).getSummaryByContractIds(List.of(5L));
    }

    @Test
    void getSummary_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/api/progress-updates/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("projectIds or contractIds")));
    }

    @Test
    void getFreelancerProjectsSummary_returnsList() throws Exception {
        when(progressUpdateService.getFreelancerProjectsSummary(7L)).thenReturn(List.of(
                ProgressSummaryItemDto.builder().projectId(3L).currentProgressPercentage(88).build()));

        mockMvc.perform(get("/api/progress-updates/freelancer/7/projects-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(3))
                .andExpect(jsonPath("$[0].currentProgressPercentage").value(88));
        verify(progressUpdateService).getFreelancerProjectsSummary(7L);
    }

    @Test
    void getLatest_withProjectId_returnsUpdate() throws Exception {
        ProgressUpdate update = progressUpdate(1L, 1L, 10L, "Latest", 80);
        when(progressUpdateService.findLatestByProjectId(1L)).thenReturn(Optional.of(update));

        mockMvc.perform(get("/api/progress-updates/latest").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Latest"));
    }

    @Test
    void getLatest_withFreelancerId_returnsUpdate() throws Exception {
        ProgressUpdate update = progressUpdate(1L, 1L, 10L, "Freelancer Latest", 75);
        when(progressUpdateService.findLatestByFreelancerId(10L)).thenReturn(Optional.of(update));

        mockMvc.perform(get("/api/progress-updates/latest").param("freelancerId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Freelancer Latest"));
    }

    @Test
    void getLatest_withContractId_returnsUpdate() throws Exception {
        ProgressUpdate update = progressUpdate(1L, 1L, 10L, "Contract Latest", 90);
        when(progressUpdateService.findLatestByContractId(5L)).thenReturn(Optional.of(update));

        mockMvc.perform(get("/api/progress-updates/latest").param("contractId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Contract Latest"));
    }

    @Test
    void getLatest_withNoParam_returns400() throws Exception {
        mockMvc.perform(get("/api/progress-updates/latest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Exactly one of")));
    }

    @Test
    void getLatest_withTwoParams_returns400() throws Exception {
        mockMvc.perform(get("/api/progress-updates/latest")
                        .param("projectId", "1")
                        .param("freelancerId", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLatest_notFound_returns404() throws Exception {
        when(progressUpdateService.findLatestByProjectId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/progress-updates/latest").param("projectId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProgressTrendByProject_returnsTrendList() throws Exception {
        when(progressUpdateService.getProgressTrendByProject(eq(1L), any(), any()))
                .thenReturn(List.of(ProgressTrendPointDto.builder().date(LocalDate.now()).progressPercentage(50).build()));

        mockMvc.perform(get("/api/progress-updates/trend/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].progressPercentage").value(50));
    }

    @Test
    void getStalledProjects_returnsList() throws Exception {
        when(progressUpdateService.getProjectIdsWithStalledProgress(7))
                .thenReturn(List.of(new StalledProjectDto(1L, LocalDateTime.now(), 30)));

        mockMvc.perform(get("/api/progress-updates/stalled/projects").param("daysWithoutUpdate", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1));
    }

    @Test
    void getDueOrOverdueProjects_returnsList() throws Exception {
        when(progressUpdateService.getProjectIdsWithStalledProgress(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/progress-updates/due-or-overdue"))
                .andExpect(status().isOk());
    }

    @Test
    void getFreelancersByActivity_returnsList() throws Exception {
        mockMvc.perform(get("/api/progress-updates/rankings/freelancers").param("limit", "10"))
                .andExpect(status().isOk());
        verify(progressUpdateService).getFreelancersByActivity(10);
    }

    @Test
    void getMostActiveProjects_returnsList() throws Exception {
        mockMvc.perform(get("/api/progress-updates/rankings/projects").param("limit", "5"))
                .andExpect(status().isOk());
        verify(progressUpdateService).getMostActiveProjects(eq(5), any(), any());
    }

    @Test
    void create_returns201AndBody() throws Exception {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setContractId(null);
        request.setFreelancerId(10L);
        request.setTitle("New");
        request.setProgressPercentage(25);
        ProgressUpdate saved = progressUpdate(1L, 1L, 10L, "New", 25);
        when(progressUpdateService.create(any())).thenReturn(saved);

        mockMvc.perform(post("/api/progress-updates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void create_whenCannotDecrease_returns400() throws Exception {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setContractId(null);
        request.setFreelancerId(10L);
        request.setTitle("New");
        request.setProgressPercentage(10);
        when(progressUpdateService.create(any())).thenThrow(new ProgressCannotDecreaseException(50, 10));

        mockMvc.perform(post("/api/progress-updates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.minAllowed").value(50))
                .andExpect(jsonPath("$.error.provided").value(10));
    }

    @Test
    void update_returns200AndBody() throws Exception {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setContractId(null);
        request.setFreelancerId(10L);
        request.setTitle("Updated");
        request.setProgressPercentage(60);
        ProgressUpdate updated = progressUpdate(1L, 1L, 10L, "Updated", 60);
        when(progressUpdateService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/progress-updates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/progress-updates/1"))
                .andExpect(status().isNoContent());
        verify(progressUpdateService).deleteById(1L);
    }

    @Test
    void getNextAllowedPercentage_withProjectId_returns200() throws Exception {
        when(progressUpdateService.getNextAllowedPercentageForProject(1L)).thenReturn(40);

        mockMvc.perform(get("/api/progress-updates/next-allowed-percentage").param("projectId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.minAllowed").value(40));
    }

    @Test
    void getNextAllowedPercentage_missingProjectId_returns400() throws Exception {
        mockMvc.perform(get("/api/progress-updates/next-allowed-percentage"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("projectId is required"));
    }

    @Test
    void validate_returnsValidationResponse() throws Exception {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);
        request.setProgressPercentage(50);
        ProgressUpdateValidationResponse response = ProgressUpdateValidationResponse.builder()
                .valid(true).minAllowed(0).provided(50).errors(List.of()).build();
        when(progressUpdateService.validate(any())).thenReturn(response);

        mockMvc.perform(post("/api/progress-updates/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    private static ProgressUpdate progressUpdate(Long id, Long projectId, Long freelancerId, String title, int pct) {
        ProgressUpdate u = new ProgressUpdate();
        u.setId(id);
        u.setProjectId(projectId);
        u.setContractId(null);
        u.setFreelancerId(freelancerId);
        u.setTitle(title);
        u.setDescription(null);
        u.setProgressPercentage(pct);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }
}
