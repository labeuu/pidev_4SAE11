package org.example.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Predictive dashboard : KPIs actuels + projections J+30 basées sur
 * les tendances historiques (régression linéaire sur les 90 derniers jours).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorAiDashboardResponse {

    // ── KPIs actuels ──────────────────────────────────────────

    private long totalAgreements;
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private long suspendedCount;
    private long expiredCount;

    private double approvalRate;
    private double avgDaysToApproval;
    private long reviewsDueNext30Days;
    private long expiringNext30Days;

    // ── Projections J+30 ─────────────────────────────────────

    private Projection projectedNewAgreements;
    private Projection projectedApprovals;
    private Projection projectedRejections;
    private Projection projectedExpirations;

    /** Taux d'approbation projeté dans 30 jours. */
    private double projectedApprovalRate;

    // ── Séries temporelles (30 derniers jours) ────────────────

    @Builder.Default
    private List<DailyCount> createdLast30Days = new ArrayList<>();
    @Builder.Default
    private List<DailyCount> approvedLast30Days = new ArrayList<>();
    @Builder.Default
    private List<DailyCount> rejectedLast30Days = new ArrayList<>();

    // ── Répartition par domaine / secteur ─────────────────────

    @Builder.Default
    private List<DomainBreakdown> byDomain = new ArrayList<>();
    @Builder.Default
    private List<DomainBreakdown> bySector = new ArrayList<>();

    // ── Alertes prédictives ───────────────────────────────────

    @Builder.Default
    private List<String> alerts = new ArrayList<>();

    // ── Inner DTOs ────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Projection {
        private long currentPeriodCount;
        private long projectedNextPeriodCount;
        /** +/- % par rapport à la période actuelle. */
        private double trendPercent;
        private String trendDirection;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCount {
        private LocalDate date;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainBreakdown {
        private String label;
        private long total;
        private long active;
    }
}
