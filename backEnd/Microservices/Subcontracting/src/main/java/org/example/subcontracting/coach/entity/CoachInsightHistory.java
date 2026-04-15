package org.example.subcontracting.coach.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "coach_insights_history", indexes = {
        @Index(name = "idx_cih_user", columnList = "user_id"),
        @Index(name = "idx_cih_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachInsightHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subcontract_id")
    private Long subcontractId;

    @Column(name = "feature_code", length = 100)
    private String featureCode;

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean free = false;

    @Column(name = "points_spent", nullable = false)
    @Builder.Default
    private Integer pointsSpent = 0;

    @Column(name = "insight_result", columnDefinition = "JSON")
    private String insightResultJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
