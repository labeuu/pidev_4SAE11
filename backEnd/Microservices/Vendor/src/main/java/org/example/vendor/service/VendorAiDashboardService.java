package org.example.vendor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vendor.dto.response.VendorAiDashboardResponse;
import org.example.vendor.dto.response.VendorAiDashboardResponse.DailyCount;
import org.example.vendor.dto.response.VendorAiDashboardResponse.DomainBreakdown;
import org.example.vendor.dto.response.VendorAiDashboardResponse.Projection;
import org.example.vendor.entity.VendorApprovalAudit;
import org.example.vendor.entity.VendorApprovalStatus;
import org.example.vendor.repository.VendorApprovalAuditRepository;
import org.example.vendor.repository.VendorApprovalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Predictive AI Dashboard — calcule les KPIs actuels et projette les tendances
 * à J+30 en utilisant une régression linéaire sur les séries temporelles
 * des 90 derniers jours du journal d'audit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VendorAiDashboardService {

    private static final int HISTORY_DAYS = 90;
    private static final int PROJECTION_DAYS = 30;

    private final VendorApprovalRepository approvalRepo;
    private final VendorApprovalAuditRepository auditRepo;

    public VendorAiDashboardResponse buildDashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since90 = now.minusDays(HISTORY_DAYS);
        LocalDateTime since30 = now.minusDays(30);
        LocalDate today = LocalDate.now();

        // ── Current KPIs ──────────────────────────────────────

        long total = approvalRepo.count();
        long pending = approvalRepo.countByStatus(VendorApprovalStatus.PENDING);
        long approved = approvalRepo.countByStatus(VendorApprovalStatus.APPROVED);
        long rejected = approvalRepo.countByStatus(VendorApprovalStatus.REJECTED);
        long suspended = approvalRepo.countByStatus(VendorApprovalStatus.SUSPENDED);
        long expired = approvalRepo.countByStatus(VendorApprovalStatus.EXPIRED);

        double approvalRate = total > 0 ? (double) approved / total * 100 : 0;

        long reviewsDue = approvalRepo.findReviewsDueBefore(today.plusDays(30)).size();
        long expiringSoon = approvalRepo.findApprovedExpiringWithin(today, today.plusDays(30)).size();

        double avgDaysToApproval = computeAvgDaysToApproval(since90);

        // ── Time-series (last 30 days) ────────────────────────

        List<DailyCount> createdSeries = buildDailySeries(auditRepo.findByActionSince("CREATED", since30));
        List<DailyCount> approvedSeries = buildDailySeries(auditRepo.findByActionSince("APPROVED", since30));
        List<DailyCount> rejectedSeries = buildDailySeries(auditRepo.findByActionSince("REJECTED", since30));

        // ── Projections J+30 (régression linéaire sur 90j) ───

        Projection projCreated = projectAction("CREATED", since90, now);
        Projection projApproved = projectAction("APPROVED", since90, now);
        Projection projRejected = projectAction("REJECTED", since90, now);
        Projection projExpired = projectAction("AUTO_EXPIRE", since90, now);

        double projectedApprovalRate = approvalRate;
        if (projCreated.getProjectedNextPeriodCount() > 0) {
            double futureApproved = approved + projApproved.getProjectedNextPeriodCount();
            double futureTotal = total + projCreated.getProjectedNextPeriodCount();
            projectedApprovalRate = futureTotal > 0 ? futureApproved / futureTotal * 100 : 0;
        }

        // ── Domain / sector breakdown ─────────────────────────

        List<DomainBreakdown> byDomain = approvalRepo.countGroupedByDomain().stream()
                .map(r -> new DomainBreakdown(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue()))
                .collect(Collectors.toList());

        List<DomainBreakdown> bySector = approvalRepo.countGroupedBySector().stream()
                .map(r -> new DomainBreakdown(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue()))
                .collect(Collectors.toList());

        // ── Predictive alerts ─────────────────────────────────

        List<String> alerts = new ArrayList<>();

        if (expiringSoon > 0) {
            alerts.add("⚠ " + expiringSoon + " agrément(s) expirent dans les 30 prochains jours.");
        }
        if (reviewsDue > 0) {
            alerts.add("📋 " + reviewsDue + " révision(s) planifiée(s) dans les 30 prochains jours.");
        }
        if (projExpired.getProjectedNextPeriodCount() > projExpired.getCurrentPeriodCount()
                && projExpired.getProjectedNextPeriodCount() > 0) {
            alerts.add("📈 Hausse prévue des expirations : ~" + projExpired.getProjectedNextPeriodCount()
                    + " d'ici 30j (vs " + projExpired.getCurrentPeriodCount() + " période précédente).");
        }
        if (projRejected.getTrendPercent() > 20) {
            alerts.add("🔴 Tendance à la hausse des rejets (+" + String.format("%.0f", projRejected.getTrendPercent())
                    + "%). Vérifiez les critères d'éligibilité.");
        }
        if (pending > approved && total > 5) {
            alerts.add("⏳ Plus d'agréments en attente (" + pending + ") que d'actifs (" + approved
                    + "). Le backlog de validation s'accumule.");
        }
        if (projApproved.getTrendPercent() < -20 && projApproved.getCurrentPeriodCount() > 0) {
            alerts.add("📉 Baisse prévue des approbations (" + String.format("%.0f", projApproved.getTrendPercent())
                    + "%). Anticipez les renouvellements.");
        }
        if (suspended > 0) {
            alerts.add("🚫 " + suspended + " agrément(s) actuellement suspendu(s).");
        }

        return VendorAiDashboardResponse.builder()
                .totalAgreements(total)
                .pendingCount(pending)
                .approvedCount(approved)
                .rejectedCount(rejected)
                .suspendedCount(suspended)
                .expiredCount(expired)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .avgDaysToApproval(Math.round(avgDaysToApproval * 10.0) / 10.0)
                .reviewsDueNext30Days(reviewsDue)
                .expiringNext30Days(expiringSoon)
                .projectedNewAgreements(projCreated)
                .projectedApprovals(projApproved)
                .projectedRejections(projRejected)
                .projectedExpirations(projExpired)
                .projectedApprovalRate(Math.round(projectedApprovalRate * 100.0) / 100.0)
                .createdLast30Days(createdSeries)
                .approvedLast30Days(approvedSeries)
                .rejectedLast30Days(rejectedSeries)
                .byDomain(byDomain)
                .bySector(bySector)
                .alerts(alerts)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    //  Projection par régression linéaire
    // ══════════════════════════════════════════════════════════

    private Projection projectAction(String action, LocalDateTime since, LocalDateTime now) {
        List<VendorApprovalAudit> events = auditRepo.findByActionSince(action, since);

        long currentPeriod = events.stream()
                .filter(e -> e.getCreatedAt().isAfter(now.minusDays(PROJECTION_DAYS)))
                .count();

        // Build weekly buckets for regression
        int weeks = HISTORY_DAYS / 7;
        double[] x = new double[weeks];
        double[] y = new double[weeks];
        for (int w = 0; w < weeks; w++) {
            x[w] = w;
            LocalDateTime weekStart = since.plusDays((long) w * 7);
            LocalDateTime weekEnd = weekStart.plusDays(7);
            final LocalDateTime ws = weekStart;
            final LocalDateTime we = weekEnd;
            y[w] = events.stream()
                    .filter(e -> !e.getCreatedAt().isBefore(ws) && e.getCreatedAt().isBefore(we))
                    .count();
        }

        double[] regression = linearRegression(x, y);
        double slope = regression[0];
        double intercept = regression[1];

        // Project 4 weeks ahead (~30 days)
        double projectedWeekly = slope * (weeks + 4) + intercept;
        long projected = Math.max(0, Math.round(projectedWeekly * 4.3));

        double trend = currentPeriod > 0
                ? ((double) projected - currentPeriod) / currentPeriod * 100
                : (projected > 0 ? 100 : 0);
        String direction = trend > 5 ? "UP" : trend < -5 ? "DOWN" : "STABLE";

        return new Projection(currentPeriod, projected, Math.round(trend * 10.0) / 10.0, direction);
    }

    /**
     * Simple linear regression: y = slope * x + intercept.
     * Returns [slope, intercept].
     */
    private double[] linearRegression(double[] x, double[] y) {
        int n = x.length;
        if (n == 0) return new double[]{0, 0};

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) {
            return new double[]{0, n > 0 ? sumY / n : 0};
        }
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    // ══════════════════════════════════════════════════════════
    //  Time-series helpers
    // ══════════════════════════════════════════════════════════

    private List<DailyCount> buildDailySeries(List<VendorApprovalAudit> events) {
        Map<LocalDate, Long> map = new LinkedHashMap<>();
        LocalDate start = LocalDate.now().minusDays(29);
        for (int d = 0; d < 30; d++) {
            map.put(start.plusDays(d), 0L);
        }
        for (VendorApprovalAudit e : events) {
            LocalDate day = e.getCreatedAt().toLocalDate();
            map.computeIfPresent(day, (k, v) -> v + 1);
        }
        return map.entrySet().stream()
                .map(e -> new DailyCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private double computeAvgDaysToApproval(LocalDateTime since) {
        List<VendorApprovalAudit> approvals = auditRepo.findByActionSince("APPROVED", since);
        if (approvals.isEmpty()) return 0;

        List<VendorApprovalAudit> creations = auditRepo.findByActionSince("CREATED", since);
        Map<Long, LocalDateTime> createdAt = new HashMap<>();
        for (VendorApprovalAudit c : creations) {
            createdAt.putIfAbsent(c.getVendorApprovalId(), c.getCreatedAt());
        }

        long totalDays = 0;
        int count = 0;
        for (VendorApprovalAudit a : approvals) {
            LocalDateTime created = createdAt.get(a.getVendorApprovalId());
            if (created != null) {
                totalDays += ChronoUnit.DAYS.between(created, a.getCreatedAt());
                count++;
            }
        }
        return count > 0 ? (double) totalDays / count : 0;
    }
}
