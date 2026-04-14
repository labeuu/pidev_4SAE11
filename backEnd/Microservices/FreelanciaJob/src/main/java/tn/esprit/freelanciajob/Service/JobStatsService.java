package tn.esprit.freelanciajob.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.response.JobStatsDTO;
import tn.esprit.freelanciajob.Dto.response.MonthlyCount;
import tn.esprit.freelanciajob.Repository.JobApplicationRepository;
import tn.esprit.freelanciajob.Repository.JobRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobStatsService {

    private final JobRepository            jobRepository;
    private final JobApplicationRepository applicationRepository;

    /**
     * Aggregates all admin KPIs in a single pass.
     * Result is cached for 5 minutes (cache TTL configured in CacheConfig).
     * Call {@code @CacheEvict(value="job-stats", allEntries=true)} after writes
     * if you want immediate invalidation.
     */
    @Cacheable("job-stats")
    public JobStatsDTO getStats() {

        long totalJobs  = jobRepository.count();
        long totalApps  = applicationRepository.count();
        double avg      = totalJobs > 0
                          ? Math.round((double) totalApps / totalJobs * 10.0) / 10.0
                          : 0.0;

        // ── Status breakdown ─────────────────────────────────────────────────
        Map<String, Long> byStatus = jobRepository.countByStatus()
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getStatus(),
                        p -> p.getCount()
                ));

        // ── Unique freelancers ────────────────────────────────────────────────
        long uniqueFreelancers = applicationRepository.countUniqueFreelancers();

        // ── Top-5 jobs ────────────────────────────────────────────────────────
        // reuses existing query (already ordered DESC by app count)
        List<JobStats> top5 = jobRepository.getJobsStatistics()
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        // ── Monthly new jobs (last 12 months) ────────────────────────────────
        List<MonthlyCount> monthly = jobRepository.getJobsPerMonth()
                .stream()
                .map(p -> new MonthlyCount(p.getMonth(), p.getCount()))
                .collect(Collectors.toList());

        return JobStatsDTO.builder()
                .totalJobs(totalJobs)
                .avgApplicationsPerJob(avg)
                .uniqueFreelancers(uniqueFreelancers)
                .jobsByStatus(byStatus)
                .top5Jobs(top5)
                .jobsPerMonth(monthly)
                .build();
    }
}
