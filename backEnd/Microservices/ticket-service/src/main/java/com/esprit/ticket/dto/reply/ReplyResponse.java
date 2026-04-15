package com.esprit.ticket.dto.reply;

import com.esprit.ticket.domain.ReplySender;

import java.time.LocalDateTime;

public record ReplyResponse(
        Long id,
        Long ticketId,
        String message,
        ReplySender sender,
        Long authorUserId,
        LocalDateTime createdAt,
        boolean readByUser,
        boolean readByAdmin
) {}
