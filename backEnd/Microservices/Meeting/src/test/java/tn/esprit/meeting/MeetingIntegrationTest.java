package tn.esprit.meeting;

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
import tn.esprit.meeting.client.ContractClient;
import tn.esprit.meeting.client.ProjectClient;
import tn.esprit.meeting.client.UserClient;
import tn.esprit.meeting.dto.*;
import tn.esprit.meeting.entity.Meeting;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.enums.MeetingType;
import tn.esprit.meeting.repository.MeetingRepository;
import tn.esprit.meeting.service.GoogleMeetService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full application context integration tests.
 *
 * {@code @SpringBootTest} starts the complete Spring context against the "test"
 * profile (H2 database, all external services disabled). Feign clients and the
 * Google Meet adapter are replaced with Mockito beans so the service can be
 * exercised end-to-end without network calls.
 *
 * Scope of coverage:
 *  - HTTP request → controller → service → repository (H2) → response
 *  - Global exception handler HTTP status mapping
 *  - Cross-concern validation (bean validation + business rules)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Meeting Microservice – Integration Tests")
class MeetingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeetingRepository meetingRepository;

    // Replace Feign clients so no discovery/network calls are made
    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private ProjectClient projectClient;

    @MockitoBean
    private ContractClient contractClient;

    // Replace Google Meet so calendar calls are never issued
    @MockitoBean
    private GoogleMeetService googleMeetService;

    private ObjectMapper objectMapper;

    private static final Long CLIENT_ID     = 10L;
    private static final Long FREELANCER_ID = 20L;
    private static final String X_USER_ID   = "X-User-Id";
    private static final LocalDateTime START = LocalDateTime.now().plusDays(1);
    private static final LocalDateTime END   = START.plusHours(1);

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Default stub for user resolution (used by toResponse())
        UserDto stubUser = new UserDto();
        stubUser.setFirstName("Test");
        stubUser.setLastName("User");
        stubUser.setEmail("test@example.com");
        lenient().when(userClient.getUserById(any())).thenReturn(stubUser);

        // Google Meet unavailable by default → Jitsi fallback
        lenient().when(googleMeetService.isAvailable()).thenReturn(false);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private Meeting persistMeeting(MeetingStatus status) {
        Meeting m = Meeting.builder()
                .clientId(CLIENT_ID)
                .freelancerId(FREELANCER_ID)
                .title("Integration Meeting")
                .agenda("Full stack test")
                .startTime(START)
                .endTime(END)
                .meetingType(MeetingType.VIDEO_CALL)
                .status(status)
                .build();
        return meetingRepository.save(m);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // End-to-end: Create → Read → Accept → Complete lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Full lifecycle: CREATE → READ → ACCEPT → COMPLETE")
    void fullMeetingLifecycle_createsAcceptsAndCompletes() throws Exception {
        // ── Step 1: CLIENT creates a meeting ──────────────────────────────────
        CreateMeetingRequest createReq = new CreateMeetingRequest();
        createReq.setFreelancerId(FREELANCER_ID);
        createReq.setTitle("Kickoff Meeting");
        createReq.setAgenda("Define scope");
        createReq.setStartTime(START);
        createReq.setEndTime(END);

        String createResponse = mockMvc.perform(post("/api/meetings")
                        .header(X_USER_ID, CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Long meetingId = objectMapper.readTree(createResponse).get("id").asLong();
        assertThat(meetingId).isPositive();

        // ── Step 2: Verify meeting is retrievable by both participants ─────────
        mockMvc.perform(get("/api/meetings/{id}", meetingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(meetingId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        List<Meeting> allForClient = meetingRepository.findAllByUserId(CLIENT_ID);
        assertThat(allForClient).hasSize(1);

        // ── Step 3: FREELANCER accepts the meeting ────────────────────────────
        StatusUpdateRequest acceptReq = new StatusUpdateRequest();
        acceptReq.setStatus(MeetingStatus.ACCEPTED);

        mockMvc.perform(patch("/api/meetings/{id}/status", meetingId)
                        .header(X_USER_ID, FREELANCER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(acceptReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.meetLink").isNotEmpty()); // Jitsi fallback

        // ── Step 4: Meeting is marked COMPLETED ───────────────────────────────
        StatusUpdateRequest completeReq = new StatusUpdateRequest();
        completeReq.setStatus(MeetingStatus.COMPLETED);

        mockMvc.perform(patch("/api/meetings/{id}/status", meetingId)
                        .header(X_USER_ID, CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // ── Verify final DB state ─────────────────────────────────────────────
        Optional<Meeting> finalMeeting = meetingRepository.findById(meetingId);
        assertThat(finalMeeting).isPresent();
        assertThat(finalMeeting.get().getStatus()).isEqualTo(MeetingStatus.COMPLETED);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Create → Cancel by CLIENT
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CLIENT can cancel a PENDING meeting they created")
    void createAndCancel_byClient_meetingIsCancelled() throws Exception {
        // Arrange – persist a PENDING meeting directly
        Meeting m = persistMeeting(MeetingStatus.PENDING);

        StatusUpdateRequest cancelReq = new StatusUpdateRequest();
        cancelReq.setStatus(MeetingStatus.CANCELLED);
        cancelReq.setReason("Schedule conflict");

        // Act & Assert
        mockMvc.perform(patch("/api/meetings/{id}/status", m.getId())
                        .header(X_USER_ID, CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Meeting updated = meetingRepository.findById(m.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(MeetingStatus.CANCELLED);
        assertThat(updated.getCancellationReason()).isEqualTo("Schedule conflict");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Delete PENDING meeting
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CLIENT can delete a PENDING meeting; it is removed from DB")
    void deletePendingMeeting_removedFromDatabase() throws Exception {
        // Arrange
        Meeting m = persistMeeting(MeetingStatus.PENDING);

        // Act & Assert
        mockMvc.perform(delete("/api/meetings/{id}", m.getId())
                        .header(X_USER_ID, CLIENT_ID))
                .andExpect(status().isNoContent());

        assertThat(meetingRepository.findById(m.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE returns 400 when meeting is ACCEPTED (cannot delete non-PENDING)")
    void deleteAcceptedMeeting_returns400() throws Exception {
        // Arrange
        Meeting m = persistMeeting(MeetingStatus.ACCEPTED);

        // Act & Assert
        mockMvc.perform(delete("/api/meetings/{id}", m.getId())
                        .header(X_USER_ID, CLIENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PENDING")));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Bean validation at HTTP layer
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /api/meetings returns 400 when freelancerId is missing")
    void createMeeting_missingFreelancerId_returns400() throws Exception {
        // Arrange – freelancerId is @NotNull, omitted here
        CreateMeetingRequest req = new CreateMeetingRequest();
        req.setTitle("No Freelancer");
        req.setStartTime(START);
        req.setEndTime(END);
        req.setFreelancerId(null); // explicit violation

        // Act & Assert
        mockMvc.perform(post("/api/meetings")
                        .header(X_USER_ID, CLIENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Stats end-to-end
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/meetings/stats returns correct counts after seeding meetings")
    void getMeetingStats_afterSeeding_returnsCorrectCounts() throws Exception {
        // Arrange – seed 2 PENDING + 1 ACCEPTED for CLIENT_ID
        persistMeeting(MeetingStatus.PENDING);
        persistMeeting(MeetingStatus.PENDING);
        persistMeeting(MeetingStatus.ACCEPTED);

        // Act & Assert
        mockMvc.perform(get("/api/meetings/stats").header(X_USER_ID, CLIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.accepted").value(1));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Business rule: only freelancer can accept
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("ACCEPT by CLIENT (not the freelancer) returns 400")
    void acceptMeeting_byClient_returns400() throws Exception {
        // Arrange
        Meeting m = persistMeeting(MeetingStatus.PENDING);

        StatusUpdateRequest acceptReq = new StatusUpdateRequest();
        acceptReq.setStatus(MeetingStatus.ACCEPTED);

        // Act & Assert – CLIENT is not the freelancer; service should reject
        mockMvc.perform(patch("/api/meetings/{id}/status", m.getId())
                        .header(X_USER_ID, CLIENT_ID) // wrong party
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(acceptReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("freelancer")));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 404 when meeting does not exist
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /api/meetings/{id} returns 404 for unknown meeting id")
    void getMeetingById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/meetings/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
