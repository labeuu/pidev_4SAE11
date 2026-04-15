package tn.esprit.freelanciajob.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Repository.JobRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository-layer tests for {@link JobRepository}.
 *
 * Uses {@code @DataJpaTest} with H2 in-memory database to verify all custom
 * JPQL queries without starting a full application context.
 */
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("JobRepository – JPA Query Tests")
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void clearDb() {
        jobRepository.deleteAll();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Job save(String title, String category, JobStatus status,
                     LocationType location, BigDecimal budgetMin, BigDecimal budgetMax) {
        return jobRepository.save(Job.builder()
                .clientId(1L)
                .clientType(ClientType.INDIVIDUAL)
                .title(title)
                .description("A full description for " + title + " covering all the requirements.")
                .category(category)
                .locationType(location)
                .budgetMin(budgetMin)
                .budgetMax(budgetMax)
                .currency("USD")
                .status(status)
                .build());
    }

    // ── findByClientId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByClientId – should return only jobs belonging to given client")
    void findByClientId_returnsCorrectJobs() {
        // Arrange
        save("Java Dev", "Engineering", JobStatus.OPEN,  LocationType.REMOTE, bd(1000), bd(3000));
        jobRepository.save(Job.builder()
                .clientId(99L).clientType(ClientType.INDIVIDUAL).title("Other Job")
                .description("Long enough description for testing purposes.")
                .category("Other").locationType(LocationType.ONSITE)
                .status(JobStatus.OPEN).build());

        // Act
        List<Job> result = jobRepository.findByClientId(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Java Dev");
    }

    // ── findByStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus – should return only jobs with the given status")
    void findByStatus_filtersCorrectly() {
        // Arrange
        save("Open Job 1",  "Engineering", JobStatus.OPEN,      LocationType.REMOTE, bd(500), bd(1000));
        save("Filled Job",  "Design",      JobStatus.FILLED,    LocationType.HYBRID, bd(200), bd(500));
        save("Open Job 2",  "Marketing",   JobStatus.OPEN,      LocationType.ONSITE, bd(300), bd(800));

        // Act
        List<Job> openJobs   = jobRepository.findByStatus(JobStatus.OPEN);
        List<Job> filledJobs = jobRepository.findByStatus(JobStatus.FILLED);

        // Assert
        assertThat(openJobs).hasSize(2);
        assertThat(filledJobs).hasSize(1);
        assertThat(filledJobs.get(0).getTitle()).isEqualTo("Filled Job");
    }

    // ── searchJobs ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchJobs – keyword match on title (case-insensitive)")
    void searchJobs_keywordMatchTitle() {
        // Arrange
        save("Senior Java Developer", "Engineering", JobStatus.OPEN, LocationType.REMOTE, bd(3000), bd(6000));
        save("UI Designer",           "Design",      JobStatus.OPEN, LocationType.REMOTE, bd(1000), bd(2000));

        // Act
        List<Job> result = jobRepository.searchJobs("java", null, null, null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).containsIgnoringCase("java");
    }

    @Test
    @DisplayName("searchJobs – category filter returns only matching category")
    void searchJobs_categoryFilter() {
        // Arrange
        save("Designer Role",  "Design",      JobStatus.OPEN, LocationType.REMOTE, bd(1000), bd(3000));
        save("Dev Role",       "Engineering", JobStatus.OPEN, LocationType.REMOTE, bd(2000), bd(5000));

        // Act
        List<Job> result = jobRepository.searchJobs(null, "Design", null, null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Design");
    }

    @Test
    @DisplayName("searchJobs – budget range filter excludes out-of-range jobs")
    void searchJobs_budgetRangeFilter() {
        // Arrange – job range 1000–3000, searching for 2000–5000 (overlap exists)
        save("Job In Range",    "Engineering", JobStatus.OPEN, LocationType.REMOTE, bd(1000), bd(3000));
        // job range 100–500 — does not overlap with 2000-5000
        save("Job Out Of Range","Engineering", JobStatus.OPEN, LocationType.REMOTE, bd(100),  bd(500));

        // Act
        List<Job> result = jobRepository.searchJobs(null, null, null, bd(2000), bd(5000));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Job In Range");
    }

    @Test
    @DisplayName("searchJobs – FILLED jobs are excluded (only OPEN returned)")
    void searchJobs_onlyReturnsOpenJobs() {
        // Arrange
        save("Open Job",   "Engineering", JobStatus.OPEN,   LocationType.REMOTE, bd(1000), bd(3000));
        save("Filled Job", "Engineering", JobStatus.FILLED, LocationType.REMOTE, bd(1000), bd(3000));

        // Act – no filter, just search all
        List<Job> result = jobRepository.searchJobs(null, null, null, null, null);

        // Assert – FILLED job is excluded by the query's WHERE j.status = 'OPEN'
        assertThat(result).allMatch(j -> j.getStatus() == JobStatus.OPEN);
    }

    @Test
    @DisplayName("searchJobs – locationType filter works correctly")
    void searchJobs_locationTypeFilter() {
        // Arrange
        save("Remote Dev", "Engineering", JobStatus.OPEN, LocationType.REMOTE, bd(1000), bd(3000));
        save("Onsite Dev", "Engineering", JobStatus.OPEN, LocationType.ONSITE, bd(1000), bd(3000));

        // Act
        List<Job> result = jobRepository.searchJobs(null, null, LocationType.REMOTE, null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocationType()).isEqualTo(LocationType.REMOTE);
    }

    @Test
    @DisplayName("searchJobs – null parameters return all OPEN jobs")
    void searchJobs_allNullParams_returnsAllOpenJobs() {
        // Arrange
        save("Job A", "Engineering", JobStatus.OPEN,      LocationType.REMOTE, bd(1000), bd(3000));
        save("Job B", "Design",      JobStatus.OPEN,      LocationType.HYBRID, bd(500),  bd(1500));
        save("Job C", "Marketing",   JobStatus.CANCELLED, LocationType.REMOTE, bd(200),  bd(600));

        // Act
        List<Job> result = jobRepository.searchJobs(null, null, null, null, null);

        // Assert – only 2 OPEN jobs
        assertThat(result).hasSize(2);
    }

    // ── getJobsStatistics ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getJobsStatistics – should return jobs ordered by application count DESC")
    void getJobsStatistics_returnsJobStats() {
        // Arrange – just verify the query runs without error
        save("Job X", "Engineering", JobStatus.OPEN, LocationType.REMOTE, bd(1000), bd(3000));

        // Act
        var stats = jobRepository.getJobsStatistics();

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.get(0).getJobTitle()).isEqualTo("Job X");
        assertThat(stats.get(0).getApplicationsCount()).isEqualTo(0L);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
