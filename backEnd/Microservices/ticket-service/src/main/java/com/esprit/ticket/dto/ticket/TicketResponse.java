package com.esprit.ticket.dto.ticket;

import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        Long userId,
        String subject,
        TicketStatus status,
        TicketPriority priority,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt,
        LocalDateTime firstResponseAt,
        LocalDateTime resolvedAt,
        Long responseTimeMinutes,
        int reopenCount,
        boolean canReopen
) {}
