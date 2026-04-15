package com.esprit.ticket.entity;

import com.esprit.ticket.domain.TicketPriority;
import com.esprit.ticket.domain.TicketStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_user", columnList = "userId"),
        @Index(name = "idx_ticket_status_activity", columnList = "status,lastActivityAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketPriority priority;

    private Long assignedAdminId;

    @Column(length = 200)
    private String assignedAdminName;

    private LocalDateTime assignedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastActivityAt;

    /** First human admin reply (system auto-replies use authorUserId 0 and do not set this). */
    private LocalDateTime firstResponseAt;

    /** Set when ticket becomes CLOSED (manual or auto). */
    private LocalDateTime resolvedAt;

    /** Set when the ticket is reopened (used for list ordering: reopened first). */
    private LocalDateTime lastReopenedAt;

    /** Minutes between createdAt and firstResponseAt. */
    private Long responseTimeMinutes;

    @Column(nullable = false)
    @Builder.Default
    private int reopenCount = 0;

    @PrePersist
    void prePersist() {
        if (status == null) status = TicketStatus.OPEN;
        if (priority == null) priority = TicketPriority.MEDIUM;
        if (lastActivityAt == null) lastActivityAt = LocalDateTime.now();
    }
}

