package com.esprit.ticket.entity;

import com.esprit.ticket.domain.ReplySender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_replies", indexes = {
        @Index(name = "idx_reply_ticket", columnList = "ticket_id"),
        @Index(name = "idx_reply_author", columnList = "authorUserId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReplySender sender;

    /**
     * Numeric platform user ID of the author (resolved from JWT → email → user service).
     * Needed to enforce: only author can modify/delete (unless ADMIN override on delete).
     */
    @Column(nullable = false)
    private Long authorUserId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

