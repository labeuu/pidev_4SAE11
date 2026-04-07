package org.example.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Score de confiance d'un freelancer calculé à partir de son historique
 * d'agréments, reviews et projets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorTrustScoreResponse {

    private Long freelancerId;

    /** Score global 0-100. */
    private int score;

    /** Label lisible : Excellent / Bon / Moyen / Faible / Insuffisant. */
    private String label;

    /** Nombre total d'agréments (toutes organisations). */
    private long totalAgreements;

    /** Nombre d'agréments actuellement APPROVED. */
    private long activeAgreements;

    /** Nombre de renouvellements cumulés. */
    private long totalRenewals;

    /** Taux de renouvellement (renewals / total agreements). */
    private double renewalRate;

    /** Nombre de rejets. */
    private long rejectionCount;

    /** Nombre de suspensions. */
    private long suspensionCount;

    /** Note moyenne des reviews (client→freelancer), 0 si aucun review. */
    private double averageRating;

    /** Nombre de reviews. */
    private long reviewCount;

    /** Nombre de projets communs (tous clients). */
    private long sharedProjectCount;

    @Builder.Default
    private List<String> breakdown = new ArrayList<>();
}
