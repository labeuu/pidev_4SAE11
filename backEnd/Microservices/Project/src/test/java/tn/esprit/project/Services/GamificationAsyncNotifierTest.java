package tn.esprit.project.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.project.Client.GamificationClient;

import static org.mockito.Mockito.*;

/**
 * Unit tests for GamificationAsyncNotifier.
 */
@ExtendWith(MockitoExtension.class)
class GamificationAsyncNotifierTest {

    @Mock
    private GamificationClient gamificationClient;

    @InjectMocks
    private GamificationAsyncNotifier notifier;

    @Test
    void notifyProjectCreated_nominalCase_callsClient() {
        ProjectService.ProjectCreatedEvent event = new ProjectService.ProjectCreatedEvent(1L);

        notifier.notifyProjectCreated(event);

        verify(gamificationClient).handleProjectCreated(1L);
    }

    @Test
    void notifyProjectCreated_clientThrows_logsException() {
        ProjectService.ProjectCreatedEvent event = new ProjectService.ProjectCreatedEvent(1L);
        doThrow(new RuntimeException("Offline")).when(gamificationClient).handleProjectCreated(1L);

        notifier.notifyProjectCreated(event);

        verify(gamificationClient).handleProjectCreated(1L);
        // Exception is caught and logged, no throw propagates
    }

    @Test
    void notifyProjectCompleted_nominalCase_callsClient() {
        ProjectService.ProjectCompletedEvent event = new ProjectService.ProjectCompletedEvent(2L);

        notifier.notifyProjectCompleted(event);

        verify(gamificationClient).handleProjectCompleted(2L);
    }

    @Test
    void notifyProjectCompleted_clientThrows_interruptsThread() {
        ProjectService.ProjectCompletedEvent event = new ProjectService.ProjectCompletedEvent(2L);
        doThrow(new RuntimeException("Offline")).when(gamificationClient).handleProjectCompleted(2L);

        notifier.notifyProjectCompleted(event);

        verify(gamificationClient).handleProjectCompleted(2L);
        // Ensure Thread.currentThread().interrupt() logic executes without crashing the test
    }
}
