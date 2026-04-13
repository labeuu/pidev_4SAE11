package com.esprit.ticket.controller;

import com.esprit.ticket.dto.ticket.CreateTicketRequest;
import com.esprit.ticket.dto.ticket.MonthlyTicketCount;
import com.esprit.ticket.dto.ticket.TicketResponse;
import com.esprit.ticket.dto.ticket.TicketStatsResponse;
import com.esprit.ticket.dto.ticket.TicketUnreadCountEntry;
import com.esprit.ticket.dto.ticket.UpdateTicketRequest;
import com.esprit.ticket.service.TicketPdfExportService;
import com.esprit.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketPdfExportService ticketPdfExportService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketStatsResponse stats() {
        return ticketService.getStats();
    }

    @GetMapping("/stats/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MonthlyTicketCount> monthlyStats() {
        return ticketService.getMonthlyStats();
    }

    @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPdf() {
        byte[] pdf = ticketPdfExportService.buildMonthlyReportPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets-report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/unread-counts")
    public List<TicketUnreadCountEntry> unreadCounts() {
        return ticketService.unreadCountsForCurrentUser();
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest req) {
        return ticketService.create(req);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<TicketResponse> getAll() {
        return ticketService.getAll();
    }

    @GetMapping("/user/{userId}")
    public List<TicketResponse> getByUserId(@PathVariable Long userId) {
        return ticketService.getByUserId(userId);
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id) {
        ticketService.markTicketRepliesRead(id);
    }

    @PutMapping("/{id}/reopen")
    public TicketResponse reopen(@PathVariable Long id) {
        return ticketService.reopen(id);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketService.getById(id);
    }

    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest req) {
        return ticketService.update(id, req);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse close(@PathVariable Long id) {
        return ticketService.close(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        ticketService.delete(id);
    }
}
