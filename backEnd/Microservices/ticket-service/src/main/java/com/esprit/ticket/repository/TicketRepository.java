package com.esprit.ticket.repository;

import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByStatusAndLastActivityAtBefore(TicketStatus status, LocalDateTime before);
}

