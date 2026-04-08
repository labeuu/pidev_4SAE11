package com.esprit.ticket.dto.reply;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReplyRequest(
        @NotBlank(message = "message is required")
        @Size(max = 2000, message = "message must be <= 2000 characters")
        String message
) {}

