package com.esprit.ticket.repository;

import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByStatusAndLastActivityAtBefore(TicketStatus status, LocalDateTime before);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    long countByStatus(TicketStatus status);

    @Query("SELECT AVG(t.responseTimeMinutes) FROM Ticket t WHERE t.responseTimeMinutes IS NOT NULL")
    Double averageResponseTimeMinutes();

    @Query(
            value =
                    """
                    SELECT YEAR(created_at) AS y, MONTH(created_at) AS m, COUNT(*) AS c
                    FROM tickets
                    GROUP BY YEAR(created_at), MONTH(created_at)
                    ORDER BY YEAR(created_at), MONTH(created_at)
                    """,
            nativeQuery = true)
    List<Object[]> countTicketsGroupedByYearMonth();
}
