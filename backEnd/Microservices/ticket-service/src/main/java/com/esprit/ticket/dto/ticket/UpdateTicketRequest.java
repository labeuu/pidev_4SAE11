package com.esprit.ticket.dto.ticket;

import com.esprit.ticket.domain.TicketPriority;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(max = 200, message = "subject must be <= 200 characters")
        String subject,
        TicketPriority priority
) {}

