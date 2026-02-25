package org.example.offer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Enregistrement d'une vue d'offre par un client (comportement).
 * Utilisé par le Smart Matching pour les recommandations basées sur le comportement.
 */
@Entity
@Table(name = "offer_views", indexes = {
        @Index(name = "idx_offer_view_client", columnList = "clientId"),
        @Index(name = "idx_offer_view_offer", columnList = "offer_id"),
        @Index(name = "idx_offer_view_at", columnList = "viewedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }
}
