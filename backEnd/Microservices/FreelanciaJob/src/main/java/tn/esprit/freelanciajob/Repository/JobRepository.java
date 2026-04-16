package tn.esprit.freelanciajob.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.freelanciajob.Dto.JobStats;
import tn.esprit.freelanciajob.Dto.projection.MonthlyJobProjection;
import tn.esprit.freelanciajob.Dto.projection.StatusCountProjection;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    List<Job> findByClientId(Long clientId);

    List<Job> findByStatus(JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = 'OPEN' AND (" +
           "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "(:keyword IS NULL OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))))" +
           " AND (:category IS NULL OR j.category = :category)" +
           " AND (:locationType IS NULL OR j.locationType = :locationType)" +
           " AND (:budgetMin IS NULL OR j.budgetMax >= :budgetMin)" +
           " AND (:budgetMax IS NULL OR j.budgetMin <= :budgetMax)")
    List<Job> searchJobs(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("locationType") tn.esprit.freelanciajob.Entity.Enums.LocationType locationType,
        @Param("budgetMin") java.math.BigDecimal budgetMin,
        @Param("budgetMax") java.math.BigDecimal budgetMax
    );

    @Query("SELECT new tn.esprit.freelanciajob.Dto.JobStats(j.id, j.title, COUNT(a)) " +
           "FROM Job j LEFT JOIN j.applications a " +
           "GROUP BY j.id, j.title " +
           "ORDER BY COUNT(a) DESC")
    List<JobStats> getJobsStatistics();

    // ── Admin stats ───────────────────────────────────────────────────────────

    /** Count jobs grouped by status (JPQL interface projection). */
    @Query("SELECT CAST(j.status AS string) AS status, COUNT(j) AS count FROM Job j GROUP BY j.status")
    List<StatusCountProjection> countByStatus();

    /**
     * Count jobs created per calendar month for the last 12 months.
     * Native query — uses MySQL DATE_FORMAT for "YYYY-MM" labels.
     */
    @Query(value =
        "SELECT DATE_FORMAT(j.created_at, '%Y-%m') AS month, COUNT(*) AS count " +
        "FROM jobs j " +
        "WHERE j.created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
        "GROUP BY month " +
        "ORDER BY month ASC",
        nativeQuery = true)
    List<MonthlyJobProjection> getJobsPerMonth();
}
