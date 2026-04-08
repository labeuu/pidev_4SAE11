package com.esprit.ticket.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "subject is required")
        @Size(max = 200, message = "subject must be <= 200 characters")
        String subject
) {}

