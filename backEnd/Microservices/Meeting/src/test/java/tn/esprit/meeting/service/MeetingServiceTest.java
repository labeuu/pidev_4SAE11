package tn.esprit.meeting.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.meeting.client.UserClient;
import tn.esprit.meeting.dto.*;
import tn.esprit.meeting.entity.Meeting;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.enums.MeetingType;
import tn.esprit.meeting.exception.MeetingNotFoundException;
import tn.esprit.meeting.exception.MeetingValidationException;
import tn.esprit.meeting.repository.MeetingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MeetingService}.
 *
 * Strategy:
 *  - All external dependencies (repository, Google Meet, UserClient) are mocked.
 *  - Each test follows the Arrange / Act / Assert (AAA) pattern.
 *  - Nested classes group tests by method, making navigation easier.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingService – Unit Tests")
class MeetingServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private GoogleMeetService googleMeetService;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private MeetingService meetingService;

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private static final Long CLIENT_ID     = 1L;
    private static final Long FREELANCER_ID = 2L;
    private static final Long STRANGER_ID   = 99L;

    /** A future start time used across tests. */
    private static final LocalDateTime FUTURE_START = LocalDateTime.now().plusDays(1);
    /** One hour after FUTURE_START — a valid, 60-minute duration. */
    private static final LocalDateTime FUTURE_END   = FUTURE_START.plusHours(1);

    /** Builds a minimal {@link Meeting} with the given id and status. */
    private Meeting meeting(Long id, MeetingStatus status) {
        return Meeting.builder()
                .id(id)
                .clientId(CLIENT_ID)
                .freelancerId(FREELANCER_ID)
                .title("Project Kickoff")
                .agenda("Discuss requirements")
                .startTime(FUTURE_START)
                .endTime(FUTURE_END)
                .meetingType(MeetingType.VIDEO_CALL)
                .status(status)
                .build();
    }

    /** Builds a stub {@link CreateMeetingRequest} with valid times. */
    private CreateMeetingRequest validCreateRequest() {
        CreateMeetingRequest req = new CreateMeetingRequest();
        req.setFreelancerId(FREELANCER_ID);
        req.setTitle("Project Kickoff");
        req.setAgenda("Discuss scope and timeline");
        req.setStartTime(FUTURE_START);
        req.setEndTime(FUTURE_END);
        return req;
    }

    /** Stubs userClient so that toResponse() does not throw. */
    private void stubUserClient() {
        UserDto user = new UserDto();
        user.setId(CLIENT_ID);
        user.setFirstName("Alice");
        user.setLastName("Doe");
        user.setEmail("alice@test.com");
        lenient().when(userClient.getUserById(any())).thenReturn(user);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // createMeeting()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createMeeting()")
    class CreateMeetingTests {

        @Test
        @DisplayName("should persist and return a PENDING meeting for a valid request")
        void validRequest_returnsPendingMeetingResponse() {
            // Arrange
            CreateMeetingRequest req = validCreateRequest();
            Meeting saved = meeting(10L, MeetingStatus.PENDING);

            when(meetingRepository.save(any(Meeting.class))).thenReturn(saved);
            stubUserClient();

            // Act
            MeetingResponse response = meetingService.createMeeting(CLIENT_ID, req);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getStatus()).isEqualTo(MeetingStatus.PENDING);
            assertThat(response.getClientId()).isEqualTo(CLIENT_ID);
            assertThat(response.getFreelancerId()).isEqualTo(FREELANCER_ID);
            verify(meetingRepository).save(any(Meeting.class));
        }

        @Test
        @DisplayName("should default meetingType to VIDEO_CALL when not provided")
        void noMeetingType_savesWithVideoCallDefault() {
            // Arrange
            CreateMeetingRequest req = validCreateRequest();
            req.setMeetingType(null);

            Meeting saved = meeting(10L, MeetingStatus.PENDING);
            when(meetingRepository.save(any(Meeting.class))).thenReturn(saved);
            stubUserClient();

            // Act
            meetingService.createMeeting(CLIENT_ID, req);

            // Assert – the entity passed to save() must have VIDEO_CALL type
            verify(meetingRepository).save(argThat(m -> m.getMeetingType() == MeetingType.VIDEO_CALL));
        }

        @Test
        @DisplayName("should throw MeetingValidationException when end time is not after start time")
        void endTimeBeforeStart_throwsValidationException() {
            // Arrange
            CreateMeetingRequest req = validCreateRequest();
            req.setEndTime(FUTURE_START.minusHours(1)); // end < start

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(CLIENT_ID, req))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("End time must be after start time");

            verifyNoInteractions(meetingRepository);
        }

        @Test
        @DisplayName("should throw MeetingValidationException when duration is shorter than 15 minutes")
        void durationUnder15Minutes_throwsValidationException() {
            // Arrange
            CreateMeetingRequest req = validCreateRequest();
            req.setEndTime(FUTURE_START.plusMinutes(10)); // only 10 minutes

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(CLIENT_ID, req))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("at least 15 minutes");

            verifyNoInteractions(meetingRepository);
        }

        @Test
        @DisplayName("should throw MeetingValidationException when duration exceeds 8 hours")
        void durationOver8Hours_throwsValidationException() {
            // Arrange
            CreateMeetingRequest req = validCreateRequest();
            req.setEndTime(FUTURE_START.plusHours(9)); // 9 hours — over limit

            // Act & Assert
            assertThatThrownBy(() -> meetingService.createMeeting(CLIENT_ID, req))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("cannot exceed 8 hours");

            verifyNoInteractions(meetingRepository);
        }

        @Test
        @DisplayName("should allow an exactly 8-hour meeting (boundary value)")
        void durationExactly8Hours_savesSuccessfully() {
            // Arrange
            CreateMeetingRequest req = validCreateRequest();
            req.setEndTime(FUTURE_START.plusHours(8)); // exactly 480 min — allowed

            Meeting saved = meeting(11L, MeetingStatus.PENDING);
            when(meetingRepository.save(any())).thenReturn(saved);
            stubUserClient();

            // Act & Assert — no exception thrown
            assertThatCode(() -> meetingService.createMeeting(CLIENT_ID, req))
                    .doesNotThrowAnyException();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateMeeting()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateMeeting()")
    class UpdateMeetingTests {

        @Test
        @DisplayName("should update title and agenda when meeting is PENDING and requester is CLIENT")
        void clientUpdatesPendingMeeting_fieldsAreUpdated() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.PENDING);
            UpdateMeetingRequest req = new UpdateMeetingRequest();
            req.setTitle("Revised Title");
            req.setAgenda("Revised Agenda");

            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act
            meetingService.updateMeeting(1L, CLIENT_ID, req);

            // Assert – the entity saved must carry the new values
            verify(meetingRepository).save(argThat(m ->
                    "Revised Title".equals(m.getTitle())
                    && "Revised Agenda".equals(m.getAgenda())
            ));
        }

        @Test
        @DisplayName("should not overwrite fields that are null in the request (partial update)")
        void nullFieldsInRequest_existingValuesArePreserved() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.PENDING);
            UpdateMeetingRequest req = new UpdateMeetingRequest();
            req.setTitle(null); // only agenda is updated
            req.setAgenda("Only Agenda Changed");

            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act
            meetingService.updateMeeting(1L, CLIENT_ID, req);

            // Assert – original title is kept
            verify(meetingRepository).save(argThat(m ->
                    "Project Kickoff".equals(m.getTitle())
                    && "Only Agenda Changed".equals(m.getAgenda())
            ));
        }

        @Test
        @DisplayName("should throw MeetingValidationException when requester is not the CLIENT")
        void nonClientRequester_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.PENDING);
            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.updateMeeting(1L, FREELANCER_ID, new UpdateMeetingRequest()))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("CLIENT");
        }

        @Test
        @DisplayName("should throw MeetingValidationException when meeting status is ACCEPTED (not editable)")
        void acceptedMeeting_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.ACCEPTED);
            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.updateMeeting(1L, CLIENT_ID, new UpdateMeetingRequest()))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // updateStatus()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        private StatusUpdateRequest statusRequest(MeetingStatus status, String reason) {
            StatusUpdateRequest req = new StatusUpdateRequest();
            req.setStatus(status);
            req.setReason(reason);
            return req;
        }

        // ── ACCEPT ────────────────────────────────────────────────────────────

        @Test
        @DisplayName("ACCEPT – freelancer accepts PENDING meeting; Jitsi link set when Google unavailable")
        void freelancerAccepts_jitsiLinkSetWhenGoogleUnavailable() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.PENDING);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(googleMeetService.isAvailable()).thenReturn(false);
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act
            meetingService.updateStatus(5L, FREELANCER_ID, statusRequest(MeetingStatus.ACCEPTED, null));

            // Assert
            verify(meetingRepository).save(argThat(m ->
                    m.getStatus() == MeetingStatus.ACCEPTED
                    && m.getMeetLink() != null
                    && m.getMeetLink().startsWith("https://meet.jit.si/")
            ));
        }

        @Test
        @DisplayName("ACCEPT – should throw when the requester is NOT the freelancer")
        void acceptByNonFreelancer_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.PENDING);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.updateStatus(5L, CLIENT_ID,
                    statusRequest(MeetingStatus.ACCEPTED, null)))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("freelancer");
        }

        @Test
        @DisplayName("ACCEPT – should throw when meeting is already ACCEPTED (not PENDING)")
        void acceptNonPendingMeeting_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.ACCEPTED);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.updateStatus(5L, FREELANCER_ID,
                    statusRequest(MeetingStatus.ACCEPTED, null)))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("PENDING");
        }

        // ── DECLINE ───────────────────────────────────────────────────────────

        @Test
        @DisplayName("DECLINE – freelancer declines PENDING meeting; reason is persisted")
        void freelancerDeclines_statusAndReasonSet() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.PENDING);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act
            meetingService.updateStatus(5L, FREELANCER_ID, statusRequest(MeetingStatus.DECLINED, "Not available"));

            // Assert
            verify(meetingRepository).save(argThat(m ->
                    m.getStatus() == MeetingStatus.DECLINED
                    && "Not available".equals(m.getCancellationReason())
            ));
        }

        // ── CANCEL ────────────────────────────────────────────────────────────

        @Test
        @DisplayName("CANCEL – client cancels a PENDING meeting (no Google event to remove)")
        void clientCancelsPendingMeeting_noCalendarCall() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.PENDING);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act
            meetingService.updateStatus(5L, CLIENT_ID, statusRequest(MeetingStatus.CANCELLED, "Schedule conflict"));

            // Assert
            verify(meetingRepository).save(argThat(m -> m.getStatus() == MeetingStatus.CANCELLED));
            verify(googleMeetService, never()).cancelEvent(any(), any());
        }

        @Test
        @DisplayName("CANCEL – cancelling an ACCEPTED meeting deletes the Google Calendar event")
        void cancelAcceptedMeeting_googleCalendarEventIsDeleted() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.ACCEPTED);
            existing.setGoogleEventId("evt-abc123");
            existing.setCalendarId("primary");
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act
            meetingService.updateStatus(5L, CLIENT_ID, statusRequest(MeetingStatus.CANCELLED, "Emergency"));

            // Assert
            verify(googleMeetService).cancelEvent("primary", "evt-abc123");
        }

        @Test
        @DisplayName("CANCEL – freelancer can also cancel a meeting they are part of")
        void freelancerCancelsMeeting_succeeds() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.ACCEPTED);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act & Assert — no exception
            assertThatCode(() -> meetingService.updateStatus(5L, FREELANCER_ID,
                    statusRequest(MeetingStatus.CANCELLED, "Conflict")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CANCEL – should throw when requester is not a participant")
        void cancelByStranger_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.ACCEPTED);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.updateStatus(5L, STRANGER_ID,
                    statusRequest(MeetingStatus.CANCELLED, null)))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("participant");
        }

        @Test
        @DisplayName("CANCEL – should throw when trying to cancel a COMPLETED meeting")
        void cancelCompletedMeeting_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.COMPLETED);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.updateStatus(5L, CLIENT_ID,
                    statusRequest(MeetingStatus.CANCELLED, null)))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("completed");
        }

        // ── COMPLETE ──────────────────────────────────────────────────────────

        @Test
        @DisplayName("COMPLETE – should mark an ACCEPTED meeting as COMPLETED")
        void completeAcceptedMeeting_statusIsCompleted() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.ACCEPTED);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(meetingRepository.save(any())).thenReturn(existing);
            stubUserClient();

            // Act
            meetingService.updateStatus(5L, CLIENT_ID, statusRequest(MeetingStatus.COMPLETED, null));

            // Assert
            verify(meetingRepository).save(argThat(m -> m.getStatus() == MeetingStatus.COMPLETED));
        }

        @Test
        @DisplayName("COMPLETE – should throw when meeting is still PENDING")
        void completePendingMeeting_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(5L, MeetingStatus.PENDING);
            when(meetingRepository.findById(5L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.updateStatus(5L, CLIENT_ID,
                    statusRequest(MeetingStatus.COMPLETED, null)))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("ACCEPTED");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getById()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("should return meeting response when meeting exists")
        void existingId_returnsMeetingResponse() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.PENDING);
            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));
            stubUserClient();

            // Act
            MeetingResponse response = meetingService.getById(1L);

            // Assert
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getStatus()).isEqualTo(MeetingStatus.PENDING);
        }

        @Test
        @DisplayName("should throw MeetingNotFoundException when meeting does not exist")
        void unknownId_throwsMeetingNotFoundException() {
            // Arrange
            when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.getById(999L))
                    .isInstanceOf(MeetingNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getMeetingsForUser()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMeetingsForUser()")
    class GetMeetingsForUserTests {

        @Test
        @DisplayName("should return all meetings where user appears as client or freelancer")
        void userWithMeetings_returnsMappedList() {
            // Arrange
            List<Meeting> meetings = List.of(
                    meeting(1L, MeetingStatus.PENDING),
                    meeting(2L, MeetingStatus.ACCEPTED)
            );
            when(meetingRepository.findAllByUserId(CLIENT_ID)).thenReturn(meetings);
            stubUserClient();

            // Act
            List<MeetingResponse> result = meetingService.getMeetingsForUser(CLIENT_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(MeetingResponse::getId)
                    .containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("should return an empty list when user has no meetings")
        void userWithNoMeetings_returnsEmptyList() {
            // Arrange
            when(meetingRepository.findAllByUserId(CLIENT_ID)).thenReturn(List.of());

            // Act
            List<MeetingResponse> result = meetingService.getMeetingsForUser(CLIENT_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // deleteMeeting()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteMeeting()")
    class DeleteMeetingTests {

        @Test
        @DisplayName("should delete a PENDING meeting when requester is the CLIENT")
        void clientDeletesPendingMeeting_repositoryDeleteCalled() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.PENDING);
            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));

            // Act
            meetingService.deleteMeeting(1L, CLIENT_ID);

            // Assert
            verify(meetingRepository).delete(existing);
        }

        @Test
        @DisplayName("should throw MeetingValidationException when requester is not CLIENT")
        void freelancerTriesToDelete_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.PENDING);
            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.deleteMeeting(1L, FREELANCER_ID))
                    .isInstanceOf(MeetingValidationException.class);

            verify(meetingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw MeetingValidationException when meeting is not PENDING")
        void deleteAcceptedMeeting_throwsValidationException() {
            // Arrange
            Meeting existing = meeting(1L, MeetingStatus.ACCEPTED);
            when(meetingRepository.findById(1L)).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> meetingService.deleteMeeting(1L, CLIENT_ID))
                    .isInstanceOf(MeetingValidationException.class)
                    .hasMessageContaining("PENDING");

            verify(meetingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw MeetingNotFoundException when meeting does not exist")
        void deleteNonExistentMeeting_throwsMeetingNotFoundException() {
            // Arrange
            when(meetingRepository.findById(42L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> meetingService.deleteMeeting(42L, CLIENT_ID))
                    .isInstanceOf(MeetingNotFoundException.class);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getMeetingStats()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMeetingStats()")
    class GetMeetingStatsTests {

        @Test
        @DisplayName("should aggregate count for each status and compute correct total")
        void allStatusesPresent_returnsCorrectAggregation() {
            // Arrange
            List<Object[]> rows = List.of(
                    new Object[]{MeetingStatus.PENDING,   3L},
                    new Object[]{MeetingStatus.ACCEPTED,  2L},
                    new Object[]{MeetingStatus.COMPLETED, 5L},
                    new Object[]{MeetingStatus.CANCELLED, 1L},
                    new Object[]{MeetingStatus.DECLINED,  1L}
            );
            when(meetingRepository.countByStatusForUser(CLIENT_ID)).thenReturn(rows);

            // Act
            MeetingStatsDTO stats = meetingService.getMeetingStats(CLIENT_ID);

            // Assert
            assertThat(stats.getTotal()).isEqualTo(12L);
            assertThat(stats.getPending()).isEqualTo(3L);
            assertThat(stats.getAccepted()).isEqualTo(2L);
            assertThat(stats.getCompleted()).isEqualTo(5L);
            assertThat(stats.getCancelled()).isEqualTo(1L);
            assertThat(stats.getDeclined()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return all-zero stats when user has no meetings")
        void noMeetings_returnsAllZeroes() {
            // Arrange
            when(meetingRepository.countByStatusForUser(CLIENT_ID)).thenReturn(List.of());

            // Act
            MeetingStatsDTO stats = meetingService.getMeetingStats(CLIENT_ID);

            // Assert
            assertThat(stats.getTotal()).isZero();
            assertThat(stats.getPending()).isZero();
            assertThat(stats.getAccepted()).isZero();
            assertThat(stats.getCompleted()).isZero();
        }

        @Test
        @DisplayName("should handle partial status set (only some statuses have data)")
        void partialStatusSet_unmatchedStatusesRemainZero() {
            // Arrange
            List<Object[]> rows = List.<Object[]>of(
                    new Object[]{MeetingStatus.ACCEPTED, 4L}
            );
            when(meetingRepository.countByStatusForUser(CLIENT_ID)).thenReturn(rows);

            // Act
            MeetingStatsDTO stats = meetingService.getMeetingStats(CLIENT_ID);

            // Assert
            assertThat(stats.getTotal()).isEqualTo(4L);
            assertThat(stats.getAccepted()).isEqualTo(4L);
            assertThat(stats.getPending()).isZero();
            assertThat(stats.getCancelled()).isZero();
        }
    }
}
