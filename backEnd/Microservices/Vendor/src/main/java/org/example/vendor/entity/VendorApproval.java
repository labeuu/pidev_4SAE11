package org.example.vendor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vendor_approvals", indexes = {
        @Index(name = "idx_va_org", columnList = "organizationId"),
        @Index(name = "idx_va_freelancer", columnList = "freelancerId"),
        @Index(name = "idx_va_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Organization (client) ID is required")
    @Column(nullable = false)
    private Long organizationId;

    @NotNull(message = "Freelancer ID is required")
    @Column(nullable = false)
    private Long freelancerId;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorApprovalStatus status;

    @Column(length = 100)
    private String domain;

    private LocalDate validFrom;

    private LocalDate validUntil;

    private LocalDate nextReviewDate;

    private Long approvedBy;

    @Column(columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String suspensionReason;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime statusChangedAt;

    /** Signature électronique client (organisation) — obligatoire avant approbation admin si statut PENDING. */
    private LocalDateTime clientSignedAt;

    @Column(length = 200)
    private String clientSignerName;

    /** Signature électronique freelancer — obligatoire avant approbation admin si statut PENDING. */
    private LocalDateTime freelancerSignedAt;

    @Column(length = 200)
    private String freelancerSignerName;

    /**
     * MÉTIER 6 — Secteur métier / famille de compétences (ex. Développement, Design, Marketing).
     * Complète le champ {@code domain} pour le classement et les statistiques.
     */
    @Column(length = 100)
    private String professionalSector;

    /**
     * MÉTIER 5 — Date d’envoi du rappel « fin de validité proche » (évite les doublons).
     */
    private LocalDateTime expiryReminderSentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        statusChangedAt = LocalDateTime.now();
        if (status == null) {
            status = VendorApprovalStatus.PENDING;
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * MÉTIER 1 — Workflow : transitions de statut autorisées.
     * PENDING   → APPROVED | REJECTED
     * APPROVED  → SUSPENDED | EXPIRED
     * SUSPENDED → APPROVED | REJECTED
     * REJECTED  → PENDING   (re-soumission)
     * EXPIRED   → PENDING   (re-soumission)
     */
    public boolean canTransitionTo(VendorApprovalStatus target) {
        return switch (this.status) {
            case PENDING   -> target == VendorApprovalStatus.APPROVED || target == VendorApprovalStatus.REJECTED;
            case APPROVED  -> target == VendorApprovalStatus.SUSPENDED || target == VendorApprovalStatus.EXPIRED;
            case SUSPENDED -> target == VendorApprovalStatus.APPROVED || target == VendorApprovalStatus.REJECTED;
            case REJECTED  -> target == VendorApprovalStatus.PENDING;
            case EXPIRED   -> target == VendorApprovalStatus.PENDING;
        };
    }

    /**
     * MÉTIER 3 — Vérifier si l'agrément est actif et non expiré.
     */
    public boolean isActiveAndValid() {
        if (this.status != VendorApprovalStatus.APPROVED) return false;
        if (this.validUntil != null && LocalDate.now().isAfter(this.validUntil)) return false;
        return true;
    }

    /**
     * MÉTIER 2 — Révision en retard ?
     */
    public boolean isReviewOverdue() {
        return this.nextReviewDate != null && LocalDate.now().isAfter(this.nextReviewDate);
    }

    /**
     * MÉTIER 2 — Révision dans les 30 prochains jours ?
     */
    public boolean isReviewUpcoming() {
        if (this.nextReviewDate == null) return false;
        LocalDate in30Days = LocalDate.now().plusDays(30);
        return !LocalDate.now().isAfter(this.nextReviewDate) && !in30Days.isBefore(this.nextReviewDate);
    }
}
