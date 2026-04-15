package com.esprit.ticket.dto.ticket;

import java.util.List;

public record TicketPageResponse(
        List<TicketResponse> items,
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements
) {}
