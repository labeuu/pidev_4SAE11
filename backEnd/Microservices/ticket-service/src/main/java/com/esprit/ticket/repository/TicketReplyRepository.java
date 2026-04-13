package com.esprit.ticket.repository;

import com.esprit.ticket.domain.ReplySender;
import com.esprit.ticket.entity.TicketReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketReplyRepository extends JpaRepository<TicketReply, Long> {
    List<TicketReply> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    void deleteByTicket_Id(Long ticketId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TicketReply r SET r.readByUser = true WHERE r.ticket.id = :ticketId AND r.sender = :sender")
    int markReadByUserForSender(@Param("ticketId") Long ticketId, @Param("sender") ReplySender sender);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TicketReply r SET r.readByAdmin = true WHERE r.ticket.id = :ticketId AND r.sender = :sender")
    int markReadByAdminForSender(@Param("ticketId") Long ticketId, @Param("sender") ReplySender sender);

    @Query(
            """
            SELECT r.ticket.id, COUNT(r)
            FROM TicketReply r
            JOIN r.ticket t
            WHERE t.userId = :userId
              AND r.sender = :admin
              AND r.readByUser = false
            GROUP BY r.ticket.id
            """)
    List<Object[]> countUnreadAdminRepliesByTicketForUser(
            @Param("userId") Long userId, @Param("admin") ReplySender admin);

    @Query(
            """
            SELECT r.ticket.id, COUNT(r)
            FROM TicketReply r
            WHERE r.sender = :userSender
              AND r.readByAdmin = false
            GROUP BY r.ticket.id
            """)
    List<Object[]> countUnreadUserRepliesByTicketForAdmin(@Param("userSender") ReplySender userSender);
}
