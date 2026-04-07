package org.example.subcontracting.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MÉTIER 5 — Journal d'audit : trace chaque action sur une sous-traitance.
 */
@Entity
@Table(name = "subcontract_audits", indexes = {
        @Index(name = "idx_audit_subcontract", columnList = "subcontractId"),
        @Index(name = "idx_audit_actor", columnList = "actorUserId"),
        @Index(name = "idx_audit_created", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subcontractId;

    private Long actorUserId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 30)
    private String fromStatus;

    @Column(length = 30)
    private String toStatus;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(length = 200)
    private String targetEntity;

    private Long targetEntityId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
