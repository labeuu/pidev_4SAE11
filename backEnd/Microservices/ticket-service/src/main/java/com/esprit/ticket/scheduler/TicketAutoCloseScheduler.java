package com.esprit.ticket.scheduler;

import com.esprit.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketAutoCloseScheduler {

    private final TicketService ticketService;

    @Scheduled(cron = "${app.ticket.auto-close-cron:0 */15 * * * *}")
    public void autoCloseInactiveTickets() {
        int closed = ticketService.autoCloseInactiveOpenTickets();
        if (closed > 0) {
            log.info("Auto-closed {} inactive tickets", closed);
        }
    }
}

