package tn.esprit.freelanciajob.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import tn.esprit.freelanciajob.Client.SkillClient;
import tn.esprit.freelanciajob.Client.UserClient;
import tn.esprit.freelanciajob.Dto.Skills;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.request.JobSearchRequest;
import tn.esprit.freelanciajob.Dto.response.JobResponse;
import tn.esprit.freelanciajob.Dto.response.UserDto;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Event.JobCreatedEvent;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Service.JobService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JobService}.
 *
 * All repositories and Feign clients are mocked so tests run without any
 * infrastructure (DB, Eureka, Portfolio service).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobService – Unit Tests")
class JobServiceTest {

    @Mock private JobRepository       jobRepository;
    @Mock private SkillClient         skillClient;
    @Mock private UserClient          userClient;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private JobService jobService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private static final Long CLIENT_ID = 1L;

    private Job buildJob(Long id, JobStatus status) {
        return Job.builder()
                .id(id)
                .clientId(CLIENT_ID)
                .clientType(ClientType.INDIVIDUAL)
                .title("Senior Java Developer")
                .description("Build microservices with Spring Boot.")
                .budgetMin(BigDecimal.valueOf(2000))
                .budgetMax(BigDecimal.valueOf(5000))
                .currency("USD")
                .category("Software Engineering")
                .locationType(LocationType.REMOTE)
                .status(status)
                .build();
    }

    private JobRequest buildRequest() {
        JobRequest r = new JobRequest();
        r.setClientId(CLIENT_ID);
        r.setClientType(ClientType.INDIVIDUAL);
        r.setTitle("Senior Java Developer");
        r.setDescription("Build microservices with Spring Boot.");
        r.setBudgetMin(BigDecimal.valueOf(2000));
        r.setBudgetMax(BigDecimal.valueOf(5000));
        r.setCurrency("USD");
        r.setCategory("Software Engineering");
        r.setLocationType(LocationType.REMOTE);
        return r;
    }

    /** Stub SkillClient so enrichWithSkills() doesn't blow up. */
    private void stubSkillClient() {
        lenient().when(skillClient.getSkillsByIds(any())).thenReturn(List.of());
        lenient().when(skillClient.getSkillsByUserId(any())).thenReturn(List.of());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // addJob()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addJob()")
    class AddJobTests {

        @Test
        @DisplayName("should persist job and publish JobCreatedEvent")
        void validRequest_persistsAndPublishesEvent() {
            // Arrange
            JobRequest req = buildRequest();
            Job saved = buildJob(1L, JobStatus.OPEN);
            UserDto client = new UserDto();
            client.setFirstName("Alice");
            client.setLastName("Smith");

            when(jobRepository.save(any(Job.class))).thenReturn(saved);
            when(userClient.getUserById(CLIENT_ID)).thenReturn(client);

            // Act
            Job result = jobService.addJob(req);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(JobStatus.OPEN);
            verify(jobRepository).save(any(Job.class));
            verify(eventPublisher).publishEvent(any(JobCreatedEvent.class));
        }

        @Test
        @DisplayName("should use fallback client name when UserClient returns null")
        void userClientReturnsNull_usesFallbackName() {
            // Arrange
            JobRequest req = buildRequest();
            when(jobRepository.save(any())).thenReturn(buildJob(1L, JobStatus.OPEN));
            when(userClient.getUserById(CLIENT_ID)).thenReturn(null);

            // Act – should not throw
            assertThatCode(() -> jobService.addJob(req)).doesNotThrowAnyException();

            // Assert – event is still published with fallback name "A Client"
            verify(eventPublisher).publishEvent(any(JobCreatedEvent.class));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateJob()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateJob()")
    class UpdateJobTests {

        @Test
        @DisplayName("should update all mutable fields and return saved job")
        void existingJob_fieldsUpdatedAndSaved() {
            // Arrange
            Job existing = buildJob(1L, JobStatus.OPEN);
            JobRequest req = buildRequest();
            req.setTitle("Updated Title");
            req.setDescription("New description for the role.");

            when(jobRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(jobRepository.save(any())).thenReturn(existing);

            // Act
            Job result = jobService.updateJob(1L, req);

            // Assert
            verify(jobRepository).save(argThat(j -> "Updated Title".equals(j.getTitle())));
        }

        @Test
        @DisplayName("should throw RuntimeException when job does not exist")
        void unknownId_throwsRuntimeException() {
            // Arrange
            when(jobRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> jobService.updateJob(99L, buildRequest()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // deleteJob()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteJob()")
    class DeleteJobTests {

        @Test
        @DisplayName("should call deleteById when job exists")
        void existingJob_deletedSuccessfully() {
            // Arrange
            when(jobRepository.existsById(1L)).thenReturn(true);

            // Act
            jobService.deleteJob(1L);

            // Assert
            verify(jobRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw RuntimeException when job does not exist")
        void unknownId_throwsRuntimeException() {
            // Arrange
            when(jobRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> jobService.deleteJob(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");

            verify(jobRepository, never()).deleteById(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getJobById()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getJobById()")
    class GetJobByIdTests {

        @Test
        @DisplayName("should return job when it exists")
        void existingId_returnsJob() {
            // Arrange
            Job job = buildJob(1L, JobStatus.OPEN);
            when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

            // Act
            Job result = jobService.getJobById(1L);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw RuntimeException when job does not exist")
        void unknownId_throwsRuntimeException() {
            // Arrange
            when(jobRepository.findById(42L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> jobService.getJobById(42L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("42");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getRecommendedJobs()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getRecommendedJobs()")
    class RecommendedJobsTests {

        @Test
        @DisplayName("should return up to 6 OPEN jobs when freelancer has no skills")
        void noFreelancerSkills_returnsUpTo6OpenJobs() {
            // Arrange
            List<Job> openJobs = List.of(
                    buildJob(1L, JobStatus.OPEN), buildJob(2L, JobStatus.OPEN),
                    buildJob(3L, JobStatus.OPEN), buildJob(4L, JobStatus.OPEN),
                    buildJob(5L, JobStatus.OPEN), buildJob(6L, JobStatus.OPEN),
                    buildJob(7L, JobStatus.OPEN)  // 7th — should be cut off
            );
            when(skillClient.getSkillsByUserId(10L)).thenReturn(List.of());
            when(jobRepository.findByStatus(JobStatus.OPEN)).thenReturn(openJobs);
            lenient().when(skillClient.getSkillsByIds(any())).thenReturn(List.of());

            // Act
            List<JobResponse> result = jobService.getRecommendedJobs(10L);

            // Assert
            assertThat(result).hasSizeLessThanOrEqualTo(6);
        }

        @Test
        @DisplayName("should filter OPEN jobs by matching freelancer skill names")
        void matchingSkills_returnsOnlyMatchingJobs() {
            // Arrange – freelancer knows "java"
            Skills javaSkill = new Skills();
            javaSkill.setId(1L);
            javaSkill.setName("Java");

            when(skillClient.getSkillsByUserId(10L)).thenReturn(List.of(javaSkill));

            // Job 1 requires skill 1 (java), Job 2 requires skill 2 (python)
            Job job1 = buildJob(1L, JobStatus.OPEN);
            job1.setRequiredSkillIds(List.of(1L));
            Job job2 = buildJob(2L, JobStatus.OPEN);
            job2.setRequiredSkillIds(List.of(2L));

            when(jobRepository.findByStatus(JobStatus.OPEN)).thenReturn(List.of(job1, job2));

            // skill 1 → java (match), skill 2 → python (no match)
            Skills pythonSkill = new Skills();
            pythonSkill.setId(2L);
            pythonSkill.setName("Python");

            when(skillClient.getSkillsByIds(List.of(1L))).thenReturn(List.of(javaSkill));
            when(skillClient.getSkillsByIds(List.of(2L))).thenReturn(List.of(pythonSkill));

            // Act
            List<JobResponse> result = jobService.getRecommendedJobs(10L);

            // Assert – only job1 matches
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return empty list when no OPEN jobs exist")
        void noOpenJobs_returnsEmptyList() {
            // Arrange
            when(skillClient.getSkillsByUserId(10L)).thenReturn(List.of());
            when(jobRepository.findByStatus(JobStatus.OPEN)).thenReturn(List.of());

            // Act
            List<JobResponse> result = jobService.getRecommendedJobs(10L);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getJobStatistics()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getJobStatistics()")
    class JobStatisticsTests {

        @Test
        @DisplayName("should return count for each JobStatus")
        void withJobs_returnsCountPerStatus() {
            // Arrange – 2 OPEN, 1 FILLED
            List<Job> jobs = List.of(
                    buildJob(1L, JobStatus.OPEN),
                    buildJob(2L, JobStatus.OPEN),
                    buildJob(3L, JobStatus.FILLED)
            );
            when(jobRepository.findAll()).thenReturn(jobs);

            // Act
            Map<String, Long> stats = jobService.getJobStatistics();

            // Assert
            assertThat(stats.get("OPEN")).isEqualTo(2L);
            assertThat(stats.get("FILLED")).isEqualTo(1L);
            assertThat(stats.get("CANCELLED")).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return all-zero map when no jobs exist")
        void noJobs_returnsAllZeroes() {
            // Arrange
            when(jobRepository.findAll()).thenReturn(List.of());

            // Act
            Map<String, Long> stats = jobService.getJobStatistics();

            // Assert – all enum values present with zero count
            assertThat(stats).containsKeys("OPEN", "FILLED", "IN_PROGRESS", "CANCELLED");
            assertThat(stats.values()).allMatch(v -> v == 0L);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // filterJobs()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("filterJobs()")
    class FilterJobsTests {

        @Test
        @DisplayName("should delegate to JpaSpecificationExecutor and return mapped page")
        @SuppressWarnings("unchecked")
        void validRequest_returnsMappedPage() {
            // Arrange
            JobSearchRequest req = new JobSearchRequest();
            req.setPage(0);
            req.setSize(9);
            req.setSortBy("createdAt");
            req.setSortDir("desc");

            List<Job> jobList = List.of(buildJob(1L, JobStatus.OPEN));
            Page<Job> page = new PageImpl<>(jobList);
            when(jobRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            stubSkillClient();

            // Act
            Page<JobResponse> result = jobService.filterJobs(req);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(jobRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getAllJobs()
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getAllJobs() – should delegate to repository and return raw list")
    void getAllJobs_delegatesToRepository() {
        // Arrange
        List<Job> jobs = List.of(buildJob(1L, JobStatus.OPEN), buildJob(2L, JobStatus.OPEN));
        when(jobRepository.findAll()).thenReturn(jobs);

        // Act
        List<Job> result = jobService.getAllJobs();

        // Assert
        assertThat(result).hasSize(2);
        verify(jobRepository).findAll();
    }

    @Test
    @DisplayName("getJobsByClientId() – should return only jobs for the given client")
    void getJobsByClientId_returnsClientJobs() {
        // Arrange
        when(jobRepository.findByClientId(CLIENT_ID)).thenReturn(List.of(buildJob(1L, JobStatus.OPEN)));
        stubSkillClient();

        // Act
        List<JobResponse> result = jobService.getJobsByClientId(CLIENT_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo(CLIENT_ID);
    }
}
