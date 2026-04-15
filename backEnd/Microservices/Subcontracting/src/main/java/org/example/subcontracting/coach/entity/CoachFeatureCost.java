package org.example.subcontracting.coach.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "coach_feature_costs", indexes = {
        @Index(name = "idx_cfc_code", columnList = "feature_code", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachFeatureCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_code", nullable = false, unique = true, length = 100)
    private String featureCode;

    @Column(length = 255)
    private String label;

    @Column(name = "cost_points", nullable = false)
    private Integer costPoints;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
