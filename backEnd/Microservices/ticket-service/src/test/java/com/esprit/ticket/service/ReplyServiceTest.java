package com.esprit.ticket.service;

import com.esprit.ticket.domain.ReplySender;
import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.reply.CreateReplyRequest;
import com.esprit.ticket.dto.reply.ReplyResponse;
import com.esprit.ticket.entity.Ticket;
import com.esprit.ticket.entity.TicketReply;
import com.esprit.ticket.exception.EntityNotFoundException;
import com.esprit.ticket.repository.TicketReplyRepository;
import com.esprit.ticket.repository.TicketRepository;
import com.esprit.ticket.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyServiceTest {

    @Mock
    TicketRepository ticketRepository;
    @Mock
    TicketReplyRepository replyRepository;
    @Mock
    CurrentUserService currentUserService;
    @Mock
    ContentModerationService contentModerationService;
    @Mock
    TicketNotificationService ticketNotificationService;

    @InjectMocks
    ReplyService replyService;

    @Test
    void addReply_ticketNotFound_throws() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replyService.addReply(new CreateReplyRequest(1L, "Hello")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ticket");

        verifyNoInteractions(replyRepository, ticketNotificationService);
    }

    @Test
    void addReply_nonAdminNotOwner_throwsForbidden() {
        Ticket t = Ticket.builder()
                .id(2L)
                .userId(100L)
                .subject("x")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .lastActivityAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusHours(1))
                .reopenCount(0)
                .build();
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(t));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.requireCurrentUserId()).thenReturn(200L);

        assertThatThrownBy(() -> replyService.addReply(new CreateReplyRequest(2L, "Hi")))
                .isInstanceOf(ResponseStatusException.class)
                .matches(ex -> ((ResponseStatusException) ex).getStatusCode().value() == 403);
    }

    @Test
    void addReply_closedTicket_throwsConflict() {
        Ticket t = Ticket.builder()
                .id(3L)
                .userId(100L)
                .subject("x")
                .status(TicketStatus.CLOSED)
                .priority(TicketPriority.MEDIUM)
                .lastActivityAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .reopenCount(0)
                .build();
        when(ticketRepository.findById(3L)).thenReturn(Optional.of(t));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.requireCurrentUserId()).thenReturn(100L);

        assertThatThrownBy(() -> replyService.addReply(new CreateReplyRequest(3L, "Hi")))
                .isInstanceOf(ResponseStatusException.class)
                .matches(ex -> ((ResponseStatusException) ex).getStatusCode().value() == 409);
    }

    @Test
    void addReply_userReply_savesAndNotifiesInbox() {
        LocalDateTime created = LocalDateTime.now().minusHours(2);
        Ticket t = Ticket.builder()
                .id(4L)
                .userId(100L)
                .subject("issue")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .lastActivityAt(LocalDateTime.now().minusMinutes(10))
                .createdAt(created)
                .reopenCount(0)
                .build();
        when(ticketRepository.findById(4L)).thenReturn(Optional.of(t));
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.requireCurrentUserId()).thenReturn(100L);
        when(contentModerationService.validateAndPrepareText("Hello there")).thenReturn("Hello there");
        when(replyRepository.save(any(TicketReply.class))).thenAnswer(inv -> {
            TicketReply r = inv.getArgument(0);
            r.setId(50L);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ReplyResponse response = replyService.addReply(new CreateReplyRequest(4L, "Hello there"));

        assertThat(response.message()).isEqualTo("Hello there");
        assertThat(response.sender()).isEqualTo(ReplySender.USER);
        verify(ticketNotificationService).notifyInboxAboutUserReply(t, 50L);
        ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getLastActivityAt()).isNotNull();
    }
}
