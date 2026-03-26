package com.esprit.planning.service;

import com.esprit.planning.client.ProjectClient;
import com.esprit.planning.dto.*;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.exception.EntityNotFoundException;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import com.esprit.planning.repository.ProjectDeadlineSyncRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProgressUpdateService. Verifies CRUD, finders, validation, stats, rankings,
 * and ensureProjectDeadlineInCalendarForUser with mocked repositories and clients.
 */
@ExtendWith(MockitoExtension.class)
class ProgressUpdateServiceTest {

    @Mock
    private ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private ProgressCommentRepository progressCommentRepository;

    @Mock
    private PlanningNotificationService planningNotificationService;

    @Mock
    private ProjectClient projectClient;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private ProjectDeadlineSyncRepository projectDeadlineSyncRepository;

    @InjectMocks
    private ProgressUpdateService progressUpdateService;

    @Test
    void findAll_returnsAllFromRepository() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findAll()).thenReturn(List.of(u));

        List<ProgressUpdate> result = progressUpdateService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("T");
        verify(progressUpdateRepository).findAll();
    }

    @Test
    void findById_whenFound_returnsUpdate() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Found", 75);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(u));

        ProgressUpdate result = progressUpdateService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Found");
    }

    @Test
    void findById_whenNotFound_throws() {
        when(progressUpdateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressUpdateService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ProgressUpdate not found");
    }

    @Test
    void findByProjectId_returnsListFromRepository() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        List<ProgressUpdate> result = progressUpdateService.findByProjectId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void findLatestByProjectId_whenFound_returnsOptional() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Latest", 80);
        when(progressUpdateRepository.findFirstByProjectIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(u));

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByProjectId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Latest");
    }

    @Test
    void findLatestByProjectId_whenNotFound_returnsEmpty() {
        when(progressUpdateRepository.findFirstByProjectIdOrderByCreatedAtDesc(999L)).thenReturn(Optional.empty());

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByProjectId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void getNextAllowedPercentageForProject_returnsMaxFromRepository() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 60);
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(u));

        Integer result = progressUpdateService.getNextAllowedPercentageForProject(1L);

        assertThat(result).isEqualTo(60);
    }

    @Test
    void validate_validRequest_returnsValidResponse() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getMinAllowed()).isZero();
        assertThat(result.getProvided()).isEqualTo(50);
    }

    @Test
    void validate_missingProjectId_returnsInvalid() {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setFreelancerId(10L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("projectId"));
    }

    @Test
    void validate_progressDecreases_returnsInvalid() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 70)));
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMinAllowed()).isEqualTo(70);
        assertThat(result.getErrors()).anyMatch(e -> e.contains("cannot be less"));
    }

    @Test
    void create_whenProgressDecreases_throwsProgressCannotDecreaseException() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 80)));
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "New", 30);

        assertThatThrownBy(() -> progressUpdateService.create(toCreate))
                .isInstanceOf(ProgressCannotDecreaseException.class)
                .satisfies(ex -> {
                    ProgressCannotDecreaseException e = (ProgressCannotDecreaseException) ex;
                    assertThat(e.getMinAllowed()).isEqualTo(80);
                    assertThat(e.getProvided()).isEqualTo(30);
                });
    }

    @Test
    void create_valid_savesAndReturns() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "New", 25);
        ProgressUpdate saved = progressUpdate(1L, 1L, 10L, "New", 25);
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenReturn(saved);

        ProgressUpdate result = progressUpdateService.create(toCreate);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(progressUpdateRepository).save(any(ProgressUpdate.class));
    }

    @Test
    void deleteById_delegatesToRepository() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));

        progressUpdateService.deleteById(1L);

        verify(progressUpdateRepository).deleteById(1L);
    }

    @Test
    void getProgressTrendByProject_returnsTrendFromRepository() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 40);
        u.setCreatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByProjectIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(List.of(u));

        List<ProgressTrendPointDto> result = progressUpdateService.getProgressTrendByProject(
                1L, LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getProgressPercentage()).isEqualTo(40);
    }

    @Test
    void getDashboardStatistics_returnsAggregatedStats() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findAll()).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(anyList())).thenReturn(0L);

        DashboardStatsDto result = progressUpdateService.getDashboardStatistics();

        assertThat(result.getTotalUpdates()).isEqualTo(1);
        assertThat(result.getDistinctProjectCount()).isEqualTo(1);
        assertThat(result.getDistinctFreelancerCount()).isEqualTo(1);
    }

    @Test
    void getProjectIdsWithStalledProgress_returnsStalledFromRepository() {
        LocalDateTime old = LocalDateTime.now().minusDays(10);
        Object[] row = new Object[]{1L, old};
        when(progressUpdateRepository.findProjectIdAndMaxUpdatedAt())
                .thenReturn(List.<Object[]>of(row));
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 30);
        u.setUpdatedAt(old);
        when(progressUpdateRepository.findByProjectIdAndUpdatedAt(1L, old)).thenReturn(Optional.of(u));

        List<StalledProjectDto> result = progressUpdateService.getProjectIdsWithStalledProgress(7);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
        assertThat(result.get(0).getLastProgressPercentage()).isEqualTo(30);
    }

    @Test
    void getFreelancersByActivity_returnsFromRepository() {
        when(progressUpdateRepository.findFreelancerIdAndUpdateCountOrderByCountDesc(any(PageRequest.class)))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 5L}));
        when(progressCommentRepository.countByProgressUpdate_FreelancerId(10L)).thenReturn(2L);

        List<FreelancerActivityDto> result = progressUpdateService.getFreelancersByActivity(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFreelancerId()).isEqualTo(10L);
        assertThat(result.get(0).getUpdateCount()).isEqualTo(5);
        assertThat(result.get(0).getCommentCount()).isEqualTo(2);
    }

    @Test
    void getMostActiveProjects_returnsFromRepository() {
        when(progressUpdateRepository.findProjectIdAndUpdateCountOrderByCountDescBetween(any(), any(), any(PageRequest.class)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 8L}));

        List<ProjectActivityDto> result = progressUpdateService.getMostActiveProjects(10, Optional.empty(), Optional.empty());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
        assertThat(result.get(0).getUpdateCount()).isEqualTo(8);
    }

    @Test
    void getMostActiveProjects_withDateRange_passesNonNullBoundsToRepository() {
        when(progressUpdateRepository.findProjectIdAndUpdateCountOrderByCountDescBetween(any(), any(), any(PageRequest.class)))
                .thenReturn(List.of());

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        progressUpdateService.getMostActiveProjects(5, Optional.of(from), Optional.of(to));

        verify(progressUpdateRepository).findProjectIdAndUpdateCountOrderByCountDescBetween(
                org.mockito.ArgumentMatchers.eq(from.atStartOfDay()),
                org.mockito.ArgumentMatchers.eq(to.plusDays(1).atStartOfDay()),
                any(PageRequest.class));
    }

    @Test
    void getProgressStatisticsByFreelancer_returnsDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        u.setUpdatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByFreelancerId(10L)).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(1L);

        FreelancerProgressStatsDto result = progressUpdateService.getProgressStatisticsByFreelancer(10L);

        assertThat(result.getFreelancerId()).isEqualTo(10L);
        assertThat(result.getTotalUpdates()).isEqualTo(1);
        assertThat(result.getTotalComments()).isEqualTo(1);
    }

    @Test
    void getProgressStatisticsByProject_returnsDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 60);
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(0L);

        ProjectProgressStatsDto result = progressUpdateService.getProgressStatisticsByProject(1L);

        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getUpdateCount()).isEqualTo(1);
        assertThat(result.getCurrentProgressPercentage()).isEqualTo(60);
    }

    @Test
    void findAllFilteredForExport_delegatesToRepositoryWithSpec() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(u));

        List<ProgressUpdate> result = progressUpdateService.findAllFilteredForExport(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(result).hasSize(1);
        verify(progressUpdateRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void findByContractId_returnsListFromRepository() {
        when(progressUpdateRepository.findByContractId(5L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        List<ProgressUpdate> result = progressUpdateService.findByContractId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void findByFreelancerId_returnsListFromRepository() {
        when(progressUpdateRepository.findByFreelancerId(10L)).thenReturn(List.of(progressUpdate(1L, 1L, 10L, "T", 50)));

        List<ProgressUpdate> result = progressUpdateService.findByFreelancerId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFreelancerId()).isEqualTo(10L);
    }

    @Test
    void findLatestByFreelancerId_whenFound_returnsOptional() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Latest", 80);
        when(progressUpdateRepository.findFirstByFreelancerIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(u));

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByFreelancerId(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Latest");
    }

    @Test
    void findLatestByContractId_whenFound_returnsOptional() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "Contract Latest", 90);
        when(progressUpdateRepository.findFirstByContractIdOrderByCreatedAtDesc(5L)).thenReturn(Optional.of(u));

        Optional<ProgressUpdate> result = progressUpdateService.findLatestByContractId(5L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Contract Latest");
    }

    @Test
    void update_whenProgressDecreases_throwsProgressCannotDecreaseException() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "Existing", 80);
        ProgressUpdate otherUpdate = progressUpdate(2L, 1L, 10L, "Other", 85);
        ProgressUpdate updated = progressUpdate(1L, 1L, 10L, "Updated", 30);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(existing, otherUpdate));

        assertThatThrownBy(() -> progressUpdateService.update(1L, updated))
                .isInstanceOf(ProgressCannotDecreaseException.class);
    }

    @Test
    void update_valid_savesAndReturns() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "Old", 40);
        ProgressUpdate updated = progressUpdate(1L, 1L, 10L, "Updated", 60);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(existing));
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenReturn(updated);

        ProgressUpdate result = progressUpdateService.update(1L, updated);

        assertThat(result).isNotNull();
        verify(progressUpdateRepository).save(any(ProgressUpdate.class));
    }

    @Test
    void update_withProgress100_triggersMilestoneCalendarAndFreelancerNotify() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "Almost", 90);
        existing.setUpdatedAt(LocalDateTime.now().minusHours(1));
        ProgressUpdate updated = progressUpdate(1L, 1L, 10L, "Done", 100);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(existing));
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 500L, "P", null));

        progressUpdateService.update(1L, updated);

        verify(googleCalendarService).createEventAsync(isNull(), contains("100%"), any(), any(), anyString());
        verify(planningNotificationService, atLeastOnce()).notifyUser(
                eq("10"), eq("Milestone reached"), contains("100%"), eq(PlanningNotificationService.TYPE_CALENDAR_MILESTONE), any());
    }

    @Test
    void getProgressReportForProject_returnsReportDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        u.setCreatedAt(LocalDateTime.now().minusDays(5));
        u.setUpdatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByProjectIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(2L);

        var result = progressUpdateService.getProgressReportForProject(1L, LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getUpdateCount()).isEqualTo(1);
        assertThat(result.getCommentCount()).isEqualTo(2);
        assertThat(result.getAverageProgressPercentage()).isEqualTo(50.0);
    }

    @Test
    void getProgressReportForProject_withNullDates_usesDefaults() {
        when(progressUpdateRepository.findByProjectIdAndCreatedAtBetween(eq(1L), any(), any())).thenReturn(List.of());

        var result = progressUpdateService.getProgressReportForProject(1L, null, null);

        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getUpdateCount()).isZero();
    }

    @Test
    void getProgressStatisticsByContract_returnsDto() {
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 70);
        when(progressUpdateRepository.findByContractId(5L)).thenReturn(List.of(u));
        when(progressCommentRepository.countByProgressUpdate_IdIn(List.of(1L))).thenReturn(0L);

        var result = progressUpdateService.getProgressStatisticsByContract(5L);

        assertThat(result.getContractId()).isEqualTo(5L);
        assertThat(result.getUpdateCount()).isEqualTo(1);
        assertThat(result.getCurrentProgressPercentage()).isEqualTo(70);
    }

    @Test
    void validate_missingFreelancerId_returnsInvalid() {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setProgressPercentage(50);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("freelancerId"));
    }

    @Test
    void validate_invalidProgressRange_returnsInvalid() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);
        request.setProgressPercentage(150);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("between 0 and 100"));
    }

    @Test
    void validate_nullProgress_returnsInvalid() {
        ProgressUpdateRequest request = new ProgressUpdateRequest();
        request.setProjectId(1L);
        request.setFreelancerId(10L);

        ProgressUpdateValidationResponse result = progressUpdateService.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("progressPercentage"));
    }

    @Test
    void getMaxProgressPercentageForProject_excludesUpdateId() {
        ProgressUpdate u1 = progressUpdate(1L, 1L, 10L, "U1", 60);
        ProgressUpdate u2 = progressUpdate(2L, 1L, 10L, "U2", 80);
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(u1, u2));

        int result = progressUpdateService.getMaxProgressPercentageForProject(1L, 2L);

        assertThat(result).isEqualTo(60);
    }

    @Test
    void ensureProjectDeadlineInCalendarForUser_whenAlreadySynced_returnsEarly() {
        when(projectDeadlineSyncRepository.findByProjectId(1L)).thenReturn(
                Optional.of(com.esprit.planning.entity.ProjectDeadlineSync.builder()
                        .projectId(1L).calendarEventId("evt1").syncedAt(LocalDateTime.now()).build()));

        progressUpdateService.ensureProjectDeadlineInCalendarForUser(1L, 10L);

        verify(projectDeadlineSyncRepository).findByProjectId(1L);
        verify(projectClient, never()).getProjectById(any());
    }

    @Test
    void findAllFiltered_returnsPageFromRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        ProgressUpdate u = progressUpdate(1L, 1L, 10L, "T", 50);
        when(progressUpdateRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(u)));

        Page<ProgressUpdate> result = progressUpdateService.findAllFiltered(
                Optional.of(1L), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        verify(progressUpdateRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void create_notifiesClientWhenProjectHasClientId() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "New", 25);
        ProgressUpdate saved = progressUpdate(1L, 1L, 10L, "New", 25);
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenReturn(saved);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 99L, "Proj", null));

        progressUpdateService.create(toCreate);

        verify(planningNotificationService, atLeastOnce()).notifyUser(
                eq("99"), eq("New progress update"), eq("New"), eq(PlanningNotificationService.TYPE_PROGRESS_UPDATE), any());
    }

    @Test
    void create_skipsClientNotificationWhenFreelancerIsClient() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "Solo", 30);
        ProgressUpdate saved = progressUpdate(1L, 1L, 10L, "Solo", 30);
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenReturn(saved);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 10L, "Proj", null));

        progressUpdateService.create(toCreate);

        verify(planningNotificationService, never()).notifyUser(eq("10"), anyString(), anyString(), anyString(), any());
    }

    @Test
    void create_withProgress100_triggersMilestoneAsyncAndFreelancerNotify() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "Done", 100);
        ProgressUpdate saved = progressUpdate(1L, 1L, 10L, "Done", 100);
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenReturn(saved);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 200L, "P", null));

        progressUpdateService.create(toCreate);

        verify(googleCalendarService).createEventAsync(isNull(), contains("100%"), any(), any(), anyString());
        verify(planningNotificationService, atLeastOnce()).notifyUser(
                eq("10"), eq("Milestone reached"), contains("100%"), eq(PlanningNotificationService.TYPE_CALENDAR_MILESTONE), any());
    }

    @Test
    void create_withNextUpdateDue_syncsCalendarEventAndNotifiesFreelancer() {
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of());
        LocalDateTime nextDue = LocalDateTime.now().plusDays(3);
        ProgressUpdate toCreate = progressUpdate(null, 1L, 10L, "With due", 40);
        toCreate.setNextUpdateDue(nextDue);
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenAnswer(inv -> {
            ProgressUpdate p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
        when(googleCalendarService.createEvent(isNull(), anyString(), any(), any(), anyString()))
                .thenReturn(Optional.of("cal-next-1"));
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 300L, "P", null));

        progressUpdateService.create(toCreate);

        verify(googleCalendarService).createEvent(isNull(), contains("Next progress update due"), eq(nextDue), any(), anyString());
        verify(planningNotificationService, atLeastOnce()).notifyUser(
                eq("10"), eq("Calendar reminder"), contains("Next progress update due"),
                eq(PlanningNotificationService.TYPE_CALENDAR_REMINDER), any());
    }

    @Test
    void update_withChangedNextDue_deletesOldCalendarEvent() {
        LocalDateTime oldDue = LocalDateTime.now().minusDays(1);
        LocalDateTime newDue = LocalDateTime.now().plusDays(5);
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "E", 50);
        existing.setNextUpdateDue(oldDue);
        existing.setNextDueCalendarEventId("old-event");
        ProgressUpdate payload = progressUpdate(1L, 1L, 10L, "E", 55);
        payload.setNextUpdateDue(newDue);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressUpdateRepository.findByProjectId(1L)).thenReturn(List.of(existing));
        when(progressUpdateRepository.save(any(ProgressUpdate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(googleCalendarService.createEvent(isNull(), anyString(), any(), any(), anyString()))
                .thenReturn(Optional.of("new-event"));
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 400L, "P", null));

        progressUpdateService.update(1L, payload);

        verify(googleCalendarService).deleteEventAsync(isNull(), eq("old-event"));
        verify(googleCalendarService).createEvent(isNull(), anyString(), eq(newDue), any(), anyString());
    }

    @Test
    void deleteById_deletesNextDueCalendarEventWhenPresent() {
        ProgressUpdate existing = progressUpdate(1L, 1L, 10L, "Del", 50);
        existing.setNextDueCalendarEventId("evt-to-delete");
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 77L, "P", null));

        progressUpdateService.deleteById(1L);

        verify(googleCalendarService).deleteEventAsync(isNull(), eq("evt-to-delete"));
        verify(progressUpdateRepository).deleteById(1L);
        verify(planningNotificationService).notifyUser(
                eq("77"), eq("Progress update removed"), eq("Del"), eq(PlanningNotificationService.TYPE_PROGRESS_UPDATE), any());
    }

    @Test
    void getSummaryByProjectIds_nullOrEmpty_returnsEmpty() {
        assertThat(progressUpdateService.getSummaryByProjectIds(null)).isEmpty();
        assertThat(progressUpdateService.getSummaryByProjectIds(List.of())).isEmpty();
    }

    @Test
    void getSummaryByProjectIds_returnsLatestProgressPerProject() {
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now();
        ProgressUpdate u1 = progressUpdate(1L, 1L, 10L, "A", 40);
        u1.setUpdatedAt(older);
        ProgressUpdate u2 = progressUpdate(2L, 1L, 10L, "B", 65);
        u2.setUpdatedAt(newer);
        when(progressUpdateRepository.findByProjectIdIn(List.of(1L))).thenReturn(List.of(u1, u2));

        List<ProgressSummaryItemDto> result = progressUpdateService.getSummaryByProjectIds(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
        assertThat(result.get(0).getCurrentProgressPercentage()).isEqualTo(65);
        assertThat(result.get(0).getLastUpdateAt()).isEqualTo(newer);
    }

    @Test
    void getSummaryByProjectIds_whenNoUpdatesForId_returnsNullProgress() {
        when(progressUpdateRepository.findByProjectIdIn(List.of(9L))).thenReturn(List.of());

        List<ProgressSummaryItemDto> result = progressUpdateService.getSummaryByProjectIds(List.of(9L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo(9L);
        assertThat(result.get(0).getCurrentProgressPercentage()).isNull();
        assertThat(result.get(0).getLastUpdateAt()).isNull();
    }

    @Test
    void getSummaryByContractIds_nullOrEmpty_returnsEmpty() {
        assertThat(progressUpdateService.getSummaryByContractIds(null)).isEmpty();
        assertThat(progressUpdateService.getSummaryByContractIds(List.of())).isEmpty();
    }

    @Test
    void getSummaryByContractIds_returnsLatestPerContract() {
        ProgressUpdate u1 = progressUpdateWithContract(1L, 1L, 5L, 10L, "C1", 30);
        u1.setUpdatedAt(LocalDateTime.now().minusDays(1));
        ProgressUpdate u2 = progressUpdateWithContract(2L, 1L, 5L, 10L, "C2", 80);
        u2.setUpdatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByContractIdIn(List.of(5L))).thenReturn(List.of(u1, u2));

        List<ProgressSummaryItemDto> result = progressUpdateService.getSummaryByContractIds(List.of(5L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContractId()).isEqualTo(5L);
        assertThat(result.get(0).getCurrentProgressPercentage()).isEqualTo(80);
        assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void getSummaryByContractIds_multipleContracts_mixedEmptyAndPopulated() {
        ProgressUpdate u = progressUpdateWithContract(1L, 3L, 7L, 10L, "X", 55);
        u.setUpdatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByContractIdIn(List.of(5L, 7L))).thenReturn(List.of(u));

        List<ProgressSummaryItemDto> result = progressUpdateService.getSummaryByContractIds(List.of(5L, 7L));

        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(r -> r.getContractId().equals(5L)).findFirst())
                .hasValueSatisfying(r -> assertThat(r.getCurrentProgressPercentage()).isNull());
        assertThat(result.stream().filter(r -> r.getContractId().equals(7L)).findFirst())
                .hasValueSatisfying(r -> assertThat(r.getCurrentProgressPercentage()).isEqualTo(55));
    }

    @Test
    void getFreelancerProjectsSummary_groupsByProjectWithLatestUpdate() {
        ProgressUpdate u1 = progressUpdate(1L, 10L, 5L, "Old", 20);
        u1.setUpdatedAt(LocalDateTime.now().minusDays(3));
        ProgressUpdate u2 = progressUpdate(2L, 10L, 5L, "New", 55);
        u2.setUpdatedAt(LocalDateTime.now());
        ProgressUpdate other = progressUpdate(3L, 99L, 5L, "Other", 90);
        other.setUpdatedAt(LocalDateTime.now());
        when(progressUpdateRepository.findByFreelancerId(5L)).thenReturn(List.of(u1, u2, other));

        List<ProgressSummaryItemDto> result = progressUpdateService.getFreelancerProjectsSummary(5L);

        assertThat(result).hasSize(2);
        assertThat(result.stream().filter(i -> i.getProjectId().equals(10L)).findFirst())
                .hasValueSatisfying(i -> assertThat(i.getCurrentProgressPercentage()).isEqualTo(55));
    }

    @Test
    void ensureProjectDeadlineInCalendarForUser_createsEventAndSyncWhenDeadlinePresent() {
        when(projectDeadlineSyncRepository.findByProjectId(2L)).thenReturn(Optional.empty());
        LocalDateTime deadline = LocalDateTime.now().plusWeeks(2);
        when(projectClient.getProjectById(2L)).thenReturn(new ProjectDto(2L, 1L, "Big project", deadline));
        when(googleCalendarService.createEvent(isNull(), anyString(), eq(deadline), any(), anyString()))
                .thenReturn(Optional.of("deadline-cal-1"));

        progressUpdateService.ensureProjectDeadlineInCalendarForUser(2L, 8L);

        verify(projectDeadlineSyncRepository).save(any());
        verify(planningNotificationService).notifyUser(
                eq("8"), eq("Project deadline in calendar"), anyString(),
                eq(PlanningNotificationService.TYPE_CALENDAR_DEADLINE), any());
    }

    private static ProgressUpdate progressUpdateWithContract(Long id, Long projectId, Long contractId, Long freelancerId, String title, int pct) {
        ProgressUpdate u = progressUpdate(id, projectId, freelancerId, title, pct);
        u.setContractId(contractId);
        return u;
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
