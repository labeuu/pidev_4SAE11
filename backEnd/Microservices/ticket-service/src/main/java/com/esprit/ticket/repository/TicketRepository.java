package com.esprit.ticket.repository;

import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import com.esprit.ticket.entity.Ticket;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query(
            """
            SELECT t FROM Ticket t
            WHERE (:priority IS NULL OR t.priority = :priority)
            ORDER BY CASE WHEN t.reopenCount > 0 THEN 0 ELSE 1 END ASC,
                     COALESCE(t.lastReopenedAt, t.lastActivityAt) DESC
            """)
    List<Ticket> findAllForAdmin(@Param("priority") TicketPriority priority);

    @Query(
            """
            SELECT t FROM Ticket t
            WHERE t.userId = :userId AND (:priority IS NULL OR t.priority = :priority)
            ORDER BY CASE WHEN t.reopenCount > 0 THEN 0 ELSE 1 END ASC,
                     COALESCE(t.lastReopenedAt, t.lastActivityAt) DESC
            """)
    List<Ticket> findByUserIdForList(@Param("userId") Long userId, @Param("priority") TicketPriority priority);

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

    @Query("SELECT t.priority, COUNT(t) FROM Ticket t GROUP BY t.priority")
    List<Object[]> countGroupedByPriority();

    long countByReopenCountGreaterThan(int reopenCount);

    List<Ticket> findByStatusOrderByLastActivityAtDesc(TicketStatus status, Pageable pageable);
}
