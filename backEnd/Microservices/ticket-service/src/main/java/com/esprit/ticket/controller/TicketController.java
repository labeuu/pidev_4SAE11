package com.esprit.ticket.controller;

import com.esprit.ticket.dto.ticket.CreateTicketRequest;
import com.esprit.ticket.dto.ticket.MonthlyTicketCount;
import com.esprit.ticket.dto.ticket.TicketPageResponse;
import com.esprit.ticket.dto.ticket.TicketResponse;
import com.esprit.ticket.dto.ticket.TicketStatsResponse;
import com.esprit.ticket.dto.ticket.TicketUnreadCountEntry;
import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
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
    public TicketPageResponse getAll(
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "lastActivityAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.getAll(priority, status, q, sortBy, sortDir, page, size);
    }

    @GetMapping("/user/{userId}")
    public TicketPageResponse getByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "lastActivityAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.getByUserId(userId, priority, status, q, sortBy, sortDir, page, size);
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

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse assign(@PathVariable Long id) {
        return ticketService.assignToCurrentAdmin(id);
    }

    @PutMapping("/{id}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse unassign(@PathVariable Long id) {
        return ticketService.unassign(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        ticketService.delete(id);
    }
}
