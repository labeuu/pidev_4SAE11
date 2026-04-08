package org.example.vendor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Profil de matching agrégé par freelancer — recalculé périodiquement
 * à partir des données Portfolio, Review, Contract et Vendor.
 * Les freelancers avec agrément vendor actif reçoivent un bonus de visibilité.
 */
@Entity
@Table(name = "freelancer_match_profiles", indexes = {
        @Index(name = "idx_fmp_freelancer", columnList = "freelancerId", unique = true),
        @Index(name = "idx_fmp_score", columnList = "globalScore")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerMatchProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long freelancerId;

    private String displayName;

    /** JSON array : ["Angular","Java","Spring","Design"] */
    @Column(columnDefinition = "TEXT")
    private String skillTags;

    /** Primary domain from portfolio. */
    @Column(length = 100)
    private String primaryDomain;

    private Double avgRating;

    private Long reviewCount;

    private Long completedContracts;

    /** % of on-time deliveries (0-100). */
    private Double onTimeRate;

    /** Vendor trust score (0-100), null if no vendor agreement. */
    private Integer vendorTrustScore;

    /** Number of active (APPROVED) vendor agreements. */
    private Integer activeVendorAgreements;

    /** Average response time to applications in hours. */
    private Double avgResponseTimeHours;

    /** Composite score 0-100 used for ranking. */
    @Column(nullable = false)
    private Integer globalScore = 0;

    /** Has at least one active vendor agreement → boosted in search. */
    @Column(nullable = false)
    private Boolean vendorBoosted = false;

    @Column(nullable = false)
    private LocalDateTime lastComputedAt;

    @PrePersist
    protected void onCreate() {
        if (lastComputedAt == null) lastComputedAt = LocalDateTime.now();
    }
}
