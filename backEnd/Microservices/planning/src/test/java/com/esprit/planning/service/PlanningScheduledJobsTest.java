package com.esprit.planning.service;

import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressUpdateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningScheduledJobsTest {

    @Mock
    private ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private PlanningNotificationService planningNotificationService;

    @InjectMocks
    private PlanningScheduledJobs planningScheduledJobs;

    private ProgressUpdate overdue;

    @BeforeEach
    void setUp() {
        overdue = ProgressUpdate.builder()
                .id(10L)
                .projectId(1L)
                .freelancerId(99L)
                .title("Sprint 1")
                .description("desc")
                .progressPercentage(50)
                .nextUpdateDue(LocalDateTime.now().minusDays(1))
                .nextDueOverdueNotified(false)
                .build();
    }

    @Test
    void notifyOverdueNextProgressDue_marksNotifiedAndNotifies() {
        when(progressUpdateRepository.findByNextUpdateDueIsNotNullAndNextUpdateDueBeforeAndNextDueOverdueNotifiedIsFalse(any()))
                .thenReturn(List.of(overdue));

        planningScheduledJobs.notifyOverdueNextProgressDue();

        ArgumentCaptor<ProgressUpdate> saved = ArgumentCaptor.forClass(ProgressUpdate.class);
        verify(progressUpdateRepository).save(saved.capture());
        assertThat(saved.getValue().getNextDueOverdueNotified()).isTrue();

        verify(planningNotificationService).notifyUser(
                eq("99"),
                eq("Progress update due date passed"),
                org.mockito.ArgumentMatchers.contains("Sprint 1"),
                eq(PlanningNotificationService.TYPE_PROGRESS_NEXT_DUE_OVERDUE),
                org.mockito.ArgumentMatchers.argThat((Map<String, String> m) ->
                        "1".equals(m.get("projectId")) && "10".equals(m.get("progressUpdateId"))));
    }

    @Test
    void notifyOverdueNextProgressDue_whenTitleNull_usesIdFallbackInMessage() {
        overdue.setTitle(null);
        when(progressUpdateRepository.findByNextUpdateDueIsNotNullAndNextUpdateDueBeforeAndNextDueOverdueNotifiedIsFalse(any()))
                .thenReturn(List.of(overdue));

        planningScheduledJobs.notifyOverdueNextProgressDue();

        verify(planningNotificationService).notifyUser(
                eq("99"),
                eq("Progress update due date passed"),
                org.mockito.ArgumentMatchers.contains("#10"),
                eq(PlanningNotificationService.TYPE_PROGRESS_NEXT_DUE_OVERDUE),
                any());
    }

    @Test
    void notifyOverdueNextProgressDue_whenNoRows_doesNotSaveOrNotify() {
        when(progressUpdateRepository.findByNextUpdateDueIsNotNullAndNextUpdateDueBeforeAndNextDueOverdueNotifiedIsFalse(any()))
                .thenReturn(List.of());

        planningScheduledJobs.notifyOverdueNextProgressDue();

        verify(progressUpdateRepository, never()).save(any());
        verify(planningNotificationService, never()).notifyUser(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void notifyOverdueNextProgressDue_multipleRows_notifiesEach() {
        ProgressUpdate second = ProgressUpdate.builder()
                .id(11L)
                .projectId(2L)
                .freelancerId(88L)
                .title("Other")
                .progressPercentage(30)
                .nextUpdateDue(LocalDateTime.now().minusHours(1))
                .nextDueOverdueNotified(false)
                .build();
        when(progressUpdateRepository.findByNextUpdateDueIsNotNullAndNextUpdateDueBeforeAndNextDueOverdueNotifiedIsFalse(any()))
                .thenReturn(List.of(overdue, second));

        planningScheduledJobs.notifyOverdueNextProgressDue();

        verify(progressUpdateRepository, times(2)).save(any());
        verify(planningNotificationService, times(2)).notifyUser(anyString(), anyString(), anyString(),
                eq(PlanningNotificationService.TYPE_PROGRESS_NEXT_DUE_OVERDUE), any());
    }

    @Test
    void clearOrphanNextDueCalendarEventIds_delegatesToRepository() {
        when(progressUpdateRepository.clearOrphanNextDueCalendarEventIds()).thenReturn(2);

        planningScheduledJobs.clearOrphanNextDueCalendarEventIds();

        verify(progressUpdateRepository).clearOrphanNextDueCalendarEventIds();
    }

    @Test
    void clearOrphanNextDueCalendarEventIds_whenNoneCleared_skipsLogBranch() {
        when(progressUpdateRepository.clearOrphanNextDueCalendarEventIds()).thenReturn(0);

        planningScheduledJobs.clearOrphanNextDueCalendarEventIds();

        verify(progressUpdateRepository).clearOrphanNextDueCalendarEventIds();
    }
}
