package tn.esprit.gamification.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.gamification.Dto.NotificationRequestDto;
import tn.esprit.gamification.client.NotificationClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationNotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private GamificationNotificationService service;

    @Test
    void notifyFastResponderBadge_nominalCase() {
        service.notifyFastResponderBadge(1L, "Speed Demon");

        verify(notificationClient).create(any(NotificationRequestDto.class));
    }

    @Test
    void notifyFastResponderBadge_nullUserId_doesNothing() {
        service.notifyFastResponderBadge(null, "Speed Demon");

        verify(notificationClient, never()).create(any(NotificationRequestDto.class));
    }

    @Test
    void notifyFastResponderBadge_clientThrows_swallowsException() {
        doThrow(new RuntimeException("API Down")).when(notificationClient).create(any(NotificationRequestDto.class));
        
        service.notifyFastResponderBadge(1L, "Test");
        
        verify(notificationClient).create(any(NotificationRequestDto.class));
        // test passes if no exception thrown to caller
    }

    @Test
    void notifyTopFreelancerCrowned_nominalCase() {
        service.notifyTopFreelancerCrowned(1L, 500);

        verify(notificationClient).create(any(NotificationRequestDto.class));
    }

    @Test
    void notifyTopFreelancerRevoked_nominalCase() {
        service.notifyTopFreelancerRevoked(1L);

        verify(notificationClient).create(any(NotificationRequestDto.class));
    }
}
