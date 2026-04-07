package org.example.subcontracting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.SubcontractDashboardResponse;
import org.example.subcontracting.dto.response.SubcontractorScoreResponse;
import org.example.subcontracting.entity.*;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubcontractDashboardService {

    private final SubcontractRepository subcontractRepo;
    private final SubcontractDeliverableRepository deliverableRepo;
    private final UserFeignClient userClient;

    /**
     * MÉTIER 3 — Score de performance d'un sous-traitant (0-100).
     */
    public SubcontractorScoreResponse computeScore(Long subcontractorId) {
        List<Subcontract> all = subcontractRepo.findBySubcontractorIdOrderByCreatedAtDesc(subcontractorId);

        long total = all.size();
        long completed = all.stream().filter(s -> s.getStatus() == SubcontractStatus.COMPLETED
                || s.getStatus() == SubcontractStatus.CLOSED).count();
        long cancelled = all.stream().filter(s -> s.getStatus() == SubcontractStatus.CANCELLED).count();

        long totalDel = 0, approvedDel = 0, rejectedDel = 0, overdueDel = 0;
        for (Subcontract sc : all) {
            List<SubcontractDeliverable> dels = deliverableRepo.findBySubcontractIdOrderByDeadlineAsc(sc.getId());
            totalDel += dels.size();
            for (SubcontractDeliverable d : dels) {
                if (d.getStatus() == DeliverableStatus.APPROVED) approvedDel++;
                if (d.getStatus() == DeliverableStatus.REJECTED) rejectedDel++;
                if (d.getDeadline() != null && LocalDate.now().isAfter(d.getDeadline())
                        && d.getStatus() != DeliverableStatus.APPROVED) overdueDel++;
            }
        }

        double completionRate = total > 0 ? (double) completed / total * 100 : 0;
        double onTimeRate = totalDel > 0 ? (double) (totalDel - overdueDel) / totalDel * 100 : 100;

        int score = 0;
        List<String> breakdown = new ArrayList<>();

        // Completion rate: 35 pts
        int compPts = Math.min(35, (int) (completionRate / 100 * 35));
        score += compPts;
        breakdown.add("Taux complétion: " + String.format("%.0f", completionRate) + "% → " + compPts + "/35 pts");

        // On-time rate: 30 pts
        int timePts = Math.min(30, (int) (onTimeRate / 100 * 30));
        score += timePts;
        breakdown.add("Respect deadlines: " + String.format("%.0f", onTimeRate) + "% → " + timePts + "/30 pts");

        // Deliverable approval: 20 pts
        double approvalRate = totalDel > 0 ? (double) approvedDel / totalDel * 100 : 0;
        int appPts = Math.min(20, (int) (approvalRate / 100 * 20));
        score += appPts;
        breakdown.add("Livrables approuvés: " + String.format("%.0f", approvalRate) + "% → " + appPts + "/20 pts");

        // Volume: 15 pts
        int volPts = Math.min(15, (int) (completed * 3));
        score += volPts;
        breakdown.add("Volume complété: " + completed + " → " + volPts + "/15 pts");

        // Penalty: rejections & cancellations
        int penalty = (int) (rejectedDel * 2 + cancelled * 5);
        score = Math.max(0, Math.min(100, score - penalty));
        if (penalty > 0) breakdown.add("Pénalité (rejets/annulations): -" + penalty + " pts");

        String label = scoreLabel(score);

        return SubcontractorScoreResponse.builder()
                .subcontractorId(subcontractorId)
                .subcontractorName(safeUserName(subcontractorId))
                .score(score)
                .label(label)
                .totalSubcontracts(total)
                .completedSubcontracts(completed)
                .cancelledSubcontracts(cancelled)
                .totalDeliverables(totalDel)
                .approvedDeliverables(approvedDel)
                .rejectedDeliverables(rejectedDel)
                .overdueDeliverables(overdueDel)
                .completionRate(completionRate)
                .onTimeRate(onTimeRate)
                .breakdown(breakdown)
                .build();
    }

    /**
     * MÉTIER 4 — Dashboard global.
     */
    public SubcontractDashboardResponse buildDashboard() {
        List<Subcontract> all = subcontractRepo.findAll();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (SubcontractStatus s : SubcontractStatus.values()) {
            byStatus.put(s.name(), all.stream().filter(sc -> sc.getStatus() == s).count());
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (SubcontractCategory c : SubcontractCategory.values()) {
            byCategory.put(c.name(), all.stream().filter(sc -> sc.getCategory() == c).count());
        }

        long totalDel = 0, approvedDel = 0, pendingDel = 0, overdueDel = 0;
        for (Subcontract sc : all) {
            List<SubcontractDeliverable> dels = deliverableRepo.findBySubcontractIdOrderByDeadlineAsc(sc.getId());
            totalDel += dels.size();
            for (SubcontractDeliverable d : dels) {
                if (d.getStatus() == DeliverableStatus.APPROVED) approvedDel++;
                if (d.getStatus() == DeliverableStatus.PENDING || d.getStatus() == DeliverableStatus.IN_PROGRESS)
                    pendingDel++;
                if (d.getDeadline() != null && LocalDate.now().isAfter(d.getDeadline())
                        && d.getStatus() != DeliverableStatus.APPROVED) overdueDel++;
            }
        }

        long completed = all.stream().filter(s -> s.getStatus() == SubcontractStatus.COMPLETED
                || s.getStatus() == SubcontractStatus.CLOSED).count();
        double globalRate = all.isEmpty() ? 0 : (double) completed / all.size() * 100;
        double avgDel = all.isEmpty() ? 0 : (double) totalDel / all.size();

        List<String> alerts = new ArrayList<>();
        if (overdueDel > 0) alerts.add(overdueDel + " livrable(s) en retard");
        long inProgress = byStatus.getOrDefault("IN_PROGRESS", 0L);
        if (inProgress > 5) alerts.add(inProgress + " sous-traitances en cours simultanément");
        long rejected = byStatus.getOrDefault("REJECTED", 0L);
        if (rejected > 3) alerts.add("Attention: " + rejected + " sous-traitances rejetées");

        return SubcontractDashboardResponse.builder()
                .totalSubcontracts(all.size())
                .byStatus(byStatus)
                .byCategory(byCategory)
                .totalDeliverables(totalDel)
                .approvedDeliverables(approvedDel)
                .pendingDeliverables(pendingDel)
                .overdueDeliverables(overdueDel)
                .avgDeliverablesPerSubcontract(Math.round(avgDel * 10) / 10.0)
                .globalCompletionRate(Math.round(globalRate * 10) / 10.0)
                .alerts(alerts)
                .build();
    }

    private String scoreLabel(int score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Très bon";
        if (score >= 50) return "Bon";
        if (score >= 30) return "Moyen";
        return "Insuffisant";
    }

    private String safeUserName(Long userId) {
        try {
            UserRemoteDto u = userClient.getUserById(userId);
            if (u != null) {
                String name = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                        + (u.getLastName() != null ? u.getLastName() : "")).trim();
                return name.isBlank() ? "User #" + userId : name;
            }
        } catch (Exception e) { /* fallback */ }
        return "User #" + userId;
    }
}
