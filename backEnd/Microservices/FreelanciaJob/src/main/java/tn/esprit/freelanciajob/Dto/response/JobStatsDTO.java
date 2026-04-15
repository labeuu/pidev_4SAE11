package tn.esprit.freelanciajob.Dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.freelanciajob.Dto.JobStats;

import java.util.List;
import java.util.Map;

/**
 * Flat admin statistics payload for the Freelancia Job Board.
 * Returned by GET /api/admin/job-stats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatsDTO {

    // ── KPI cards ─────────────────────────────────────────────
    private long   totalJobs;
    private double avgApplicationsPerJob;
    private long   uniqueFreelancers;

    // ── Pie chart ─────────────────────────────────────────────
    /** e.g. { "OPEN": 12, "FILLED": 4, "CANCELLED": 1 } */
    private Map<String, Long> jobsByStatus;

    // ── Bar chart ─────────────────────────────────────────────
    /** Top-5 jobs ranked by application count. */
    private List<JobStats> top5Jobs;

    // ── Line chart ────────────────────────────────────────────
    /** One entry per calendar month for the last 12 months. */
    private List<MonthlyCount> jobsPerMonth;
}
