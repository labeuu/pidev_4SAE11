package org.example.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.vendor.entity.VendorApprovalStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorApprovalResponse {

    private Long id;
    private Long organizationId;
    private Long freelancerId;
    private VendorApprovalStatus status;
    private String domain;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private LocalDate nextReviewDate;
    private Long approvedBy;
    private String approvalNotes;
    private String rejectionReason;
    private String suspensionReason;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime statusChangedAt;

    private Boolean isActive;
    private Boolean isReviewOverdue;
    private Boolean isReviewUpcoming;

    private LocalDateTime clientSignedAt;
    private String clientSignerName;
    private LocalDateTime freelancerSignedAt;
    private String freelancerSignerName;
    /** Les deux parties ont signé (prérequis approbation admin depuis PENDING). */
    private Boolean fullySigned;

    /** MÉTIER 6 — Secteur métier. */
    private String professionalSector;

    /** MÉTIER 5 — Rappel d’expiration déjà envoyé ? */
    private LocalDateTime expiryReminderSentAt;

    /** Jours restants avant validUntil (null si pas de date). */
    private Long daysUntilValidUntilExpiry;
}
