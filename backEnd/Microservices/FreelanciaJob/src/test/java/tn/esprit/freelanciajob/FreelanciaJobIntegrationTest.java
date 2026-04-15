package tn.esprit.freelanciajob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelanciajob.Client.SkillClient;
import tn.esprit.freelanciajob.Client.UserClient;
import tn.esprit.freelanciajob.Client.ExperienceClient;
import tn.esprit.freelanciajob.Dto.request.JobRequest;
import tn.esprit.freelanciajob.Dto.response.UserDto;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Entity.Enums.ClientType;
import tn.esprit.freelanciajob.Entity.Enums.JobStatus;
import tn.esprit.freelanciajob.Entity.Enums.LocationType;
import tn.esprit.freelanciajob.Repository.JobRepository;
import tn.esprit.freelanciajob.Service.EmailService;
import tn.esprit.freelanciajob.Service.FileStorageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full application context integration tests for FreelanciaJob.
 *
 * All external service calls (Feign clients, email, file storage) are replaced
 * with Mockito mocks. The DB layer uses H2 (test profile).
 *
 * Scope: HTTP → Controller → Service → Repository (H2) → response
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FreelanciaJob – Integration Tests")
class FreelanciaJobIntegrationTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private JobRepository jobRepository;

    // Mock all external dependencies
    @MockitoBean private SkillClient      skillClient;
    @MockitoBean private UserClient       userClient;
    @MockitoBean private ExperienceClient experienceClient;
    @MockitoBean private EmailService     emailService;
    @MockitoBean private FileStorageService fileStorageService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Default stubs for Feign clients
        UserDto user = new UserDto();
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@test.com");
        lenient().when(userClient.getUserById(any())).thenReturn(user);
        lenient().when(userClient.getUsersByRole(any())).thenReturn(List.of());
        lenient().when(skillClient.getSkillsByIds(any())).thenReturn(List.of());
        lenient().when(skillClient.getSkillsByUserId(any())).thenReturn(List.of());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String toJson(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private Job persistJob(JobStatus status) {
        return jobRepository.save(Job.builder()
                .clientId(1L).clientType(ClientType.INDIVIDUAL)
                .title("Integration Test Job")
                .description("A job posted for integration testing purposes.")
                .budgetMin(BigDecimal.valueOf(1000)).budgetMax(BigDecimal.valueOf(3000))
                .currency("USD").category("Engineering").locationType(LocationType.REMOTE)
                .status(status)
                .build());
    }

    private JobRequest buildJobRequest() {
        JobRequest r = new JobRequest();
        r.setClientId(1L);
        r.setClientType(ClientType.INDIVIDUAL);
        r.setTitle("Backend Developer");
        r.setDescription("We need an experienced backend developer for our platform.");
        r.setBudgetMin(BigDecimal.valueOf(2000));
        r.setBudgetMax(BigDecimal.valueOf(4000));
        r.setCurrency("USD");
        r.setCategory("Engineering");
        r.setLocationType(LocationType.REMOTE);
        return r;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Full lifecycle: CREATE → READ → UPDATE → DELETE
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Full job lifecycle: POST → GET → PUT → DELETE")
    void fullJobLifecycle() throws Exception {
        // ── Step 1: Create a job ──────────────────────────────────────────────
        String createBody = mockMvc.perform(post("/jobs/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildJobRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Backend Developer"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        Long jobId = objectMapper.readTree(createBody).get("id").asLong();
        assertThat(jobId).isPositive();

        // ── Step 2: Read the job ──────────────────────────────────────────────
        mockMvc.perform(get("/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId));

        // ── Step 3: Verify it's in the DB ─────────────────────────────────────
        Optional<Job> inDb = jobRepository.findById(jobId);
        assertThat(inDb).isPresent();
        assertThat(inDb.get().getStatus()).isEqualTo(JobStatus.OPEN);

        // ── Step 4: Update the job ────────────────────────────────────────────
        JobRequest updateReq = buildJobRequest();
        updateReq.setTitle("Senior Backend Developer");

        mockMvc.perform(put("/jobs/update/{id}", jobId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Senior Backend Developer"));

        // ── Step 5: Delete the job ────────────────────────────────────────────
        mockMvc.perform(delete("/jobs/{id}", jobId))
                .andExpect(status().isNoContent());

        assertThat(jobRepository.findById(jobId)).isEmpty();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Job statistics aggregation
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /jobs/statistics returns correct counts after seeding")
    void getStatistics_afterSeeding_returnsCorrectCounts() throws Exception {
        // Arrange – seed 3 OPEN, 1 FILLED
        persistJob(JobStatus.OPEN);
        persistJob(JobStatus.OPEN);
        persistJob(JobStatus.OPEN);
        persistJob(JobStatus.FILLED);

        // Act & Assert
        mockMvc.perform(get("/jobs/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.OPEN").value(3))
                .andExpect(jsonPath("$.FILLED").value(1));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET all jobs
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /jobs/list returns all seeded jobs")
    void getAllJobs_afterSeeding_returnsAll() throws Exception {
        // Arrange
        persistJob(JobStatus.OPEN);
        persistJob(JobStatus.OPEN);

        // Act & Assert
        mockMvc.perform(get("/jobs/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Filter jobs
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /jobs/filter returns paged results respecting page size")
    void filterJobs_respectsPageSize() throws Exception {
        // Arrange – seed 5 jobs
        for (int i = 0; i < 5; i++) persistJob(JobStatus.OPEN);

        // Filter with page size 3
        var req = new tn.esprit.freelanciajob.Dto.request.JobSearchRequest();
        req.setPage(0);
        req.setSize(3);
        req.setSortBy("id");
        req.setSortDir("asc");

        // Act & Assert
        mockMvc.perform(post("/jobs/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Delete non-existent job returns 500 (no 404 handler in this service)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /jobs/{id} for non-existent job returns 500")
    void deleteNonExistentJob_returns500() throws Exception {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mockMvc.perform(delete("/jobs/{id}", 99999L)))
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
