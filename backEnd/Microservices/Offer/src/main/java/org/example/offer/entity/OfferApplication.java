package org.example.offer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offer_applications", indexes = {
        @Index(name = "idx_offer_id", columnList = "offer_id"),
        @Index(name = "idx_client_id", columnList = "clientId"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Offer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @NotNull(message = "Client ID is required")
    @Column(nullable = false)
    private Long clientId;

    @NotBlank(message = "Message is required")
    @Size(min = 20, max = 2000, message = "Message must be between 20 and 2000 characters")
    @Column(columnDefinition = "TEXT")
    private String message;

    @NotNull(message = "Proposed budget is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Budget must be greater than 0")
    @Column(precision = 10, scale = 2)
    private BigDecimal proposedBudget;

    @Column(length = 255)
    private String portfolioUrl; // URL du portfolio

    @Column(length = 500)
    private String attachmentUrl; // Pièce jointe (CV, etc.)

    @Column(precision = 2, scale = 0)
    private Integer estimatedDuration; // Durée estimée en jours

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason; // Raison du refus

    @Column(nullable = false)
    private Boolean isRead = false; // Lu par le freelancer

    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime respondedAt;

    private LocalDateTime acceptedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        if (status == null) {
            status = ApplicationStatus.PENDING;
        }
    }

    // ========== Méthodes métier ==========

    /**
     * Accepter la candidature
     */
    public void accept() {
        this.status = ApplicationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
        this.acceptedAt = LocalDateTime.now();
    }

    /**
     * Rejeter la candidature
     */
    public void reject(String reason) {
        this.status = ApplicationStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * Marquer comme lu
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * Vérifier si la candidature peut être modifiée
     */
    public boolean canBeModified() {
        return this.status == ApplicationStatus.PENDING;
    }
}