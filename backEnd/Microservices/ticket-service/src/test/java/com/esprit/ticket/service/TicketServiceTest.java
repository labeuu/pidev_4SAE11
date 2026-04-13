package com.esprit.ticket.service;

import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.dto.ticket.TicketResponse;
import com.esprit.ticket.entity.Ticket;
import com.esprit.ticket.repository.TicketReplyRepository;
import com.esprit.ticket.repository.TicketRepository;
import com.esprit.ticket.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock TicketReplyRepository ticketReplyRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ContentModerationService contentModerationService;
    @Mock ReplyService replyService;

    @InjectMocks TicketService ticketService;

    @Test
    void reopen_setsOpenAndIncrementsCount() {
        Ticket t = Ticket.builder()
                .id(10L)
                .userId(99L)
                .subject("Help")
                .status(TicketStatus.CLOSED)
                .priority(TicketPriority.MEDIUM)
                .createdAt(LocalDateTime.now().minusDays(2))
                .lastActivityAt(LocalDateTime.now().minusHours(1))
                .reopenCount(0)
                .build();

        when(ticketRepository.findById(10L)).thenReturn(Optional.of(t));
        when(currentUserService.requireCurrentUserId()).thenReturn(99L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse r = ticketService.reopen(10L);

        assertThat(r.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(r.reopenCount()).isEqualTo(1);
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void reopen_admin_canReopenAnyTicket() {
        Ticket t = Ticket.builder()
                .id(12L)
                .userId(99L)
                .subject("Help")
                .status(TicketStatus.CLOSED)
                .priority(TicketPriority.MEDIUM)
                .createdAt(LocalDateTime.now().minusDays(2))
                .lastActivityAt(LocalDateTime.now().minusHours(1))
                .reopenCount(0)
                .build();

        when(ticketRepository.findById(12L)).thenReturn(Optional.of(t));
        when(currentUserService.requireCurrentUserId()).thenReturn(1L);
        when(currentUserService.isAdmin()).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse r = ticketService.reopen(12L);

        assertThat(r.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(r.reopenCount()).isEqualTo(1);
    }

    @Test
    void reopen_throwsWhenAlreadyUsed() {
        Ticket t = Ticket.builder()
                .id(11L)
                .userId(99L)
                .subject("Help")
                .status(TicketStatus.CLOSED)
                .priority(TicketPriority.MEDIUM)
                .createdAt(LocalDateTime.now().minusDays(2))
                .lastActivityAt(LocalDateTime.now().minusHours(1))
                .reopenCount(1)
                .build();

        when(ticketRepository.findById(11L)).thenReturn(Optional.of(t));
        when(currentUserService.requireCurrentUserId()).thenReturn(99L);

        assertThatThrownBy(() -> ticketService.reopen(11L)).isInstanceOf(ResponseStatusException.class);
    }
}
