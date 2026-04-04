package com.esprit.task.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskScheduledJobsTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskScheduledJobs taskScheduledJobs;

    @Test
    void runEscalateOverduePriorities_callsTaskService() {
        when(taskService.escalateOverduePriorities()).thenReturn(0);
        taskScheduledJobs.runEscalateOverduePriorities();
        verify(taskService).escalateOverduePriorities();
    }

    @Test
    void runEscalateOverduePriorities_whenTasksEscalated_entersInfoBranch() {
        when(taskService.escalateOverduePriorities()).thenReturn(2);
        taskScheduledJobs.runEscalateOverduePriorities();
        verify(taskService).escalateOverduePriorities();
    }

    @Test
    void runPurgeOldCancelledTasks_usesConfiguredDays() {
        ReflectionTestUtils.setField(taskScheduledJobs, "purgeCancelledDays", 30);
        when(taskService.purgeOldCancelledTasks(any(LocalDateTime.class))).thenReturn(0);
        taskScheduledJobs.runPurgeOldCancelledTasks();
        verify(taskService).purgeOldCancelledTasks(any(LocalDateTime.class));
    }

    @Test
    void runPurgeOldCancelledTasks_whenPurged_entersInfoBranch() {
        ReflectionTestUtils.setField(taskScheduledJobs, "purgeCancelledDays", 90);
        when(taskService.purgeOldCancelledTasks(any(LocalDateTime.class))).thenReturn(3);
        taskScheduledJobs.runPurgeOldCancelledTasks();
        verify(taskService).purgeOldCancelledTasks(any(LocalDateTime.class));
    }
}
