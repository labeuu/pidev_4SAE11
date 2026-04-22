package org.example.offer.service;

import org.example.offer.client.NotificationFeignClient;
import org.example.offer.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationFeignClient notificationFeignClient;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @SuppressWarnings("unchecked")
    void createNotification_withOfferAndRelated_buildsPayloadWithData() {
        when(notificationFeignClient.create(org.mockito.ArgumentMatchers.anyMap())).thenReturn(Map.of("ok", true));

        notificationService.createNotification(
                99L,
                NotificationType.APPLICATION_RECEIVED,
                "New application",
                "A freelancer applied",
                12L,
                77L
        );

        ArgumentCaptor<Map<String, Object>> captor = (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        verify(notificationFeignClient).create(captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertThat(payload.get("userId")).isEqualTo("99");
        assertThat(payload.get("title")).isEqualTo("New application");
        assertThat(payload.get("body")).isEqualTo("A freelancer applied");
        assertThat(payload.get("type")).isEqualTo("APPLICATION_RECEIVED");
        assertThat(payload).containsKey("data");
        Map<String, String> data = (Map<String, String>) payload.get("data");
        assertThat(data.get("offerId")).isEqualTo("12");
        assertThat(data.get("relatedId")).isEqualTo("77");
    }

    @Test
    void createNotification_whenClientFails_doesNotThrow() {
        when(notificationFeignClient.create(org.mockito.ArgumentMatchers.anyMap()))
                .thenThrow(new RuntimeException("notification service unavailable"));

        assertThatCode(() -> notificationService.createNotification(
                1L,
                NotificationType.GENERAL,
                "Title",
                "Body",
                null,
                null
        )).doesNotThrowAnyException();
    }
}
