package com.esprit.ticket.dto.ticket;

public record TicketStatsResponse(long total, long open, long closed, Double averageResponseTimeMinutes) {}
