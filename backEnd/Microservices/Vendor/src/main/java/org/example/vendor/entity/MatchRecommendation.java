package org.example.vendor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Recommandation : un freelancer recommandé pour un projet/offre donné.
 * Généré automatiquement par le matching engine.
 */
@Entity
@Table(name = "match_recommendations", indexes = {
        @Index(name = "idx_mr_target", columnList = "targetType,targetId"),
        @Index(name = "idx_mr_freelancer", columnList = "freelancerId"),
        @Index(name = "idx_mr_score", columnList = "matchScore")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PROJECT or OFFER. */
    @Column(nullable = false, length = 20)
    private String targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private Long freelancerId;

    private String freelancerName;

    /** Match score 0-100. */
    @Column(nullable = false)
    private Integer matchScore;

    /** JSON explanation : ["skill_match:90","rating:4.8","vendor_boost:+10"] */
    @Column(columnDefinition = "TEXT")
    private String matchReasons;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SUGGESTED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = MatchStatus.SUGGESTED;
    }

    public enum MatchStatus {
        SUGGESTED, VIEWED, CONTACTED, HIRED, DISMISSED
    }
}
