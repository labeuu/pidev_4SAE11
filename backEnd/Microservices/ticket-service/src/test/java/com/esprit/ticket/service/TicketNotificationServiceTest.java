package com.esprit.ticket.service;

import com.esprit.ticket.client.NotificationClient;
import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.notification.NotificationRequestDto;
import com.esprit.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketNotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;

    private TicketNotificationService ticketNotificationService;

    @BeforeEach
    void setUp() {
        ticketNotificationService = new TicketNotificationService(notificationClient);
    }

    @Test
    void notifyTicketCreated_sendsNotificationToTicketOwner() {
        Ticket t = Ticket.builder()
                .id(10L)
                .userId(25L)
                .subject("Payment issue")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .build();

        ticketNotificationService.notifyTicketCreated(t);

        ArgumentCaptor<NotificationRequestDto> captor = ArgumentCaptor.forClass(NotificationRequestDto.class);
        verify(notificationClient).create(captor.capture());
        NotificationRequestDto dto = captor.getValue();
        assertThat(dto.getUserId()).isEqualTo("25");
        assertThat(dto.getType()).isEqualTo(TicketNotificationService.TYPE_TICKET_CREATED);
        assertThat(dto.getTitle()).isEqualTo("Support ticket opened");
        assertThat(dto.getData().get("ticketId")).isEqualTo("10");
    }

    @Test
    void notifyInboxAboutUserReply_withoutInboxConfigured_doesNothing() {
        ReflectionTestUtils.setField(ticketNotificationService, "supportInboxUserId", "");
        Ticket t = Ticket.builder()
                .id(7L)
                .userId(25L)
                .subject("Need support")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .build();

        ticketNotificationService.notifyInboxAboutUserReply(t, 50L);

        verify(notificationClient, never()).create(any(NotificationRequestDto.class));
    }

    @Test
    void notifyTicketReopened_withInboxConfigured_sendsTwoNotifications() {
        ReflectionTestUtils.setField(ticketNotificationService, "supportInboxUserId", "100");
        Ticket t = Ticket.builder()
                .id(42L)
                .userId(25L)
                .subject("Reopen request")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.LOW)
                .build();

        ticketNotificationService.notifyTicketReopened(t);

        verify(notificationClient, times(2)).create(any(NotificationRequestDto.class));
    }
}
