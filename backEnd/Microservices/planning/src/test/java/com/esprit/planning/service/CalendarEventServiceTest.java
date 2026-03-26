package com.esprit.planning.service;

import com.esprit.planning.client.ProjectClient;
import com.esprit.planning.client.TaskClient;
import com.esprit.planning.dto.CalendarEventDto;
import com.esprit.planning.dto.ProjectDto;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressUpdateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CalendarEventService. Verifies listEventsFromDb with and without userId/role;
 * mocks ProgressUpdateRepository and ProjectClient.
 */
@ExtendWith(MockitoExtension.class)
class CalendarEventServiceTest {

    @Mock
    private ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private ProjectClient projectClient;

    @Mock
    private TaskClient taskClient;

    @InjectMocks
    private CalendarEventService calendarEventService;

    @BeforeEach
    void setUp() {
        when(taskClient.getCalendarEvents(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void listEventsFromDb_withoutUserId_returnsEventsFromUpdatesAndProjects() {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setProjectId(1L);
        pu.setNextUpdateDue(LocalDateTime.now().plusDays(1));
        pu.setTitle("Update");
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of(pu));
        when(projectClient.getProjects()).thenReturn(List.of());

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getSummary()).contains("Next progress update due");
        assertThat(result.get(0).getId()).isEqualTo("pu-1");
    }

    @Test
    void listEventsFromDb_withUserIdAndRole_callsRepositoryAndFilters() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenReturn(List.of());
        when(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(5L)).thenReturn(List.of(1L));

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1), 5L, "FREELANCER");

        assertThat(result).isNotNull();
    }

    @Test
    void listEventsFromDb_withNullTitle_usesUpdateIdInSummary() {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(2L);
        pu.setProjectId(1L);
        pu.setNextUpdateDue(LocalDateTime.now().plusDays(1));
        pu.setTitle(null);
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of(pu));
        when(projectClient.getProjects()).thenReturn(List.of());

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1));

        assertThat(result.get(0).getSummary()).contains("Update #2");
    }

    @Test
    void listEventsFromDb_withNullNextUpdateDue_skipsUpdate() {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setNextUpdateDue(null);
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of(pu));
        when(projectClient.getProjects()).thenReturn(List.of());

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1));

        assertThat(result).isEmpty();
    }

    @Test
    void listEventsFromDb_withProjectDeadlines_inRange_returnsEvents() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        LocalDateTime now = LocalDateTime.now();
        ProjectDto p = new ProjectDto();
        p.setId(1L);
        p.setTitle("Project Alpha");
        p.setDeadline(now.plusDays(2));
        when(projectClient.getProjects()).thenReturn(List.of(p));

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummary()).contains("Project deadline");
        assertThat(result.get(0).getId()).isEqualTo("proj-1");
    }

    @Test
    void listEventsFromDb_withProjectBlankTitle_usesProjectId() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        LocalDateTime now = LocalDateTime.now();
        ProjectDto p = new ProjectDto();
        p.setId(2L);
        p.setTitle("   ");
        p.setDeadline(now.plusDays(1));
        when(projectClient.getProjects()).thenReturn(List.of(p));

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result.get(0).getSummary()).contains("Project #2");
    }

    @Test
    void listEventsFromDb_withProjectNullDeadline_skipsProject() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        ProjectDto p = new ProjectDto();
        p.setId(1L);
        p.setDeadline(null);
        when(projectClient.getProjects()).thenReturn(List.of(p));

        LocalDateTime now = LocalDateTime.now();
        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).isEmpty();
    }

    @Test
    void listEventsFromDb_withProjectDeadlineOutsideRange_skipsProject() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        LocalDateTime now = LocalDateTime.now();
        ProjectDto p1 = new ProjectDto();
        p1.setId(1L);
        p1.setDeadline(now.minusDays(1));
        ProjectDto p2 = new ProjectDto();
        p2.setId(2L);
        p2.setDeadline(now.plusMonths(2));
        when(projectClient.getProjects()).thenReturn(List.of(p1, p2));

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).isEmpty();
    }

    @Test
    void listEventsFromDb_withUserIdAndRole_freelancerVisible_returnsEvent() {
        LocalDateTime now = LocalDateTime.now();
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setProjectId(1L);
        pu.setFreelancerId(5L);
        pu.setNextUpdateDue(now.plusDays(1));
        pu.setTitle("Update");
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of(pu));
        when(projectClient.getProjects()).thenReturn(List.of());
        when(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(5L)).thenReturn(List.of());

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1), 5L, "FREELANCER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("pu-1");
    }

    @Test
    void listEventsFromDb_withUserIdAndRole_clientVisibleViaProject_returnsEvent() {
        LocalDateTime now = LocalDateTime.now();
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setProjectId(10L);
        pu.setFreelancerId(99L);
        pu.setNextUpdateDue(now.plusDays(1));
        pu.setTitle("Update");
        ProjectDto proj = new ProjectDto();
        proj.setId(10L);
        proj.setClientId(5L);
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of(pu));
        when(projectClient.getProjects()).thenReturn(List.of());
        when(projectClient.getProjectById(10L)).thenReturn(proj);
        when(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(5L)).thenReturn(List.of());

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1), 5L, "CLIENT");

        assertThat(result).hasSize(1);
    }

    @Test
    void listEventsFromDb_withUserIdAndRole_projectDeadlineVisibleToClient_returnsEvent() {
        LocalDateTime now = LocalDateTime.now();
        ProjectDto p = new ProjectDto();
        p.setId(1L);
        p.setClientId(5L);
        p.setTitle("Client Project");
        p.setDeadline(now.plusDays(3));
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenReturn(List.of(p));
        when(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(5L)).thenReturn(List.of());

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1), 5L, "CLIENT");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("proj-1");
    }

    @Test
    void listEventsFromDb_withUserIdAndRole_projectDeadlineVisibleToFreelancer_returnsEvent() {
        LocalDateTime now = LocalDateTime.now();
        ProjectDto p = new ProjectDto();
        p.setId(1L);
        p.setClientId(99L);
        p.setTitle("Freelancer Project");
        p.setDeadline(now.plusDays(3));
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenReturn(List.of(p));
        when(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(5L)).thenReturn(List.of(1L));

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1), 5L, "FREELANCER");

        assertThat(result).hasSize(1);
    }

    @Test
    void listEventsFromDb_whenRepositoryThrows_returnsEventsFromProjects() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenThrow(new RuntimeException("DB error"));
        LocalDateTime now = LocalDateTime.now();
        ProjectDto p = new ProjectDto();
        p.setId(1L);
        p.setDeadline(now.plusDays(1));
        when(projectClient.getProjects()).thenReturn(List.of(p));

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).hasSize(1);
    }

    @Test
    void listEventsFromDb_whenProjectClientReturnsNull_handlesGracefully() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenReturn(null);

        LocalDateTime now = LocalDateTime.now();
        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).isEmpty();
    }

    @Test
    void listEventsFromDb_whenProjectClientThrows_handlesGracefully() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenThrow(new RuntimeException("Project service down"));

        LocalDateTime now = LocalDateTime.now();
        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).isEmpty();
    }

    @Test
    void listEventsFromDb_twoArgOverload_delegatesWithNullUser() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenReturn(List.of());

        LocalDateTime min = LocalDateTime.now();
        LocalDateTime max = min.plusDays(7);
        calendarEventService.listEventsFromDb(min, max);

        verify(taskClient).getCalendarEvents(any(), any(), isNull());
    }

    @Test
    void listEventsFromDb_whenGetProjectByIdThrows_excludesUpdateForFilteringUser() {
        LocalDateTime now = LocalDateTime.now();
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setProjectId(10L);
        pu.setFreelancerId(99L);
        pu.setNextUpdateDue(now.plusDays(1));
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of(pu));
        when(projectClient.getProjects()).thenReturn(List.of());
        when(projectClient.getProjectById(10L)).thenThrow(new RuntimeException("unavailable"));
        when(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(5L)).thenReturn(List.of());

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1), 5L, "CLIENT");

        assertThat(result).isEmpty();
    }

    @Test
    void listEventsFromDb_mergesTaskServiceEvents() {
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenReturn(List.of());
        CalendarEventDto taskEv = CalendarEventDto.builder()
                .id("task-1")
                .summary("Task due")
                .start(LocalDateTime.now().plusDays(2))
                .end(LocalDateTime.now().plusDays(2).plusHours(1))
                .build();
        when(taskClient.getCalendarEvents(any(), any(), isNull())).thenReturn(List.of(taskEv));

        LocalDateTime now = LocalDateTime.now();
        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).extracting(CalendarEventDto::getId).contains("task-1");
    }

    @Test
    void listEventsFromDb_withUserId_hidesProjectDeadlineWhenNotClientNorAssignedFreelancer() {
        LocalDateTime now = LocalDateTime.now();
        ProjectDto p = new ProjectDto();
        p.setId(2L);
        p.setClientId(99L);
        p.setTitle("Other");
        p.setDeadline(now.plusDays(2));
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of());
        when(projectClient.getProjects()).thenReturn(List.of(p));
        when(progressUpdateRepository.findDistinctProjectIdsByFreelancerId(5L)).thenReturn(List.of(1L));

        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1), 5L, "FREELANCER");

        assertThat(result).isEmpty();
    }

    @Test
    void listEventsFromDb_whenTaskClientThrows_stillReturnsOtherEvents() {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setProjectId(1L);
        pu.setNextUpdateDue(LocalDateTime.now().plusDays(1));
        pu.setTitle("U");
        when(progressUpdateRepository.findByNextUpdateDueBetween(any(), any())).thenReturn(List.of(pu));
        when(projectClient.getProjects()).thenReturn(List.of());
        when(taskClient.getCalendarEvents(any(), any(), isNull())).thenThrow(new RuntimeException("task down"));

        LocalDateTime now = LocalDateTime.now();
        List<CalendarEventDto> result = calendarEventService.listEventsFromDb(now, now.plusMonths(1));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getId()).startsWith("pu-");
    }
}
