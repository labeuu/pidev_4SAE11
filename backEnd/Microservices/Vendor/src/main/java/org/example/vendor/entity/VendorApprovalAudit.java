package org.example.vendor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MÉTIER 4 — Journal d'audit append-only : chaque transition ou action métier est enregistrée.
 */
@Entity
@Table(name = "vendor_approval_audits", indexes = {
        @Index(name = "idx_vaa_vendor", columnList = "vendorApprovalId"),
        @Index(name = "idx_vaa_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorApprovalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long vendorApprovalId;

    @Enumerated(EnumType.STRING)
    private VendorApprovalStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorApprovalStatus toStatus;

    /** Ex. CREATED, APPROVED, REJECTED, SUSPENDED, RESUBMIT, RENEW, AUTO_EXPIRE, DELETED */
    @Column(nullable = false, length = 40)
    private String action;

    /** Null = action système (expiration automatique). */
    private Long actorUserId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
