package tn.esprit.meeting.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.meeting.entity.Meeting;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.enums.MeetingType;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository slice tests for {@link MeetingRepository}.
 *
 * Uses {@code @DataJpaTest} which auto-configures an H2 in-memory database and
 * applies {@code create-drop} DDL. Only JPA infrastructure is loaded; no web or
 * service layer is involved.
 *
 * These tests verify the custom JPQL queries defined in the repository.
 */
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("MeetingRepository – JPA Query Tests")
class MeetingRepositoryTest {

    @Autowired
    private MeetingRepository meetingRepository;

    // ── Shared participants ───────────────────────────────────────────────────

    private static final Long CLIENT_ID     = 1L;
    private static final Long FREELANCER_ID = 2L;
    private static final Long OTHER_USER_ID = 99L;

    private static final LocalDateTime NOW = LocalDateTime.now();

    @BeforeEach
    void clearDatabase() {
        meetingRepository.deleteAll();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Persists and returns a {@link Meeting} with all required fields filled.
     */
    private Meeting save(Long clientId, Long freelancerId,
                         MeetingStatus status,
                         LocalDateTime start, LocalDateTime end) {
        Meeting m = Meeting.builder()
                .clientId(clientId)
                .freelancerId(freelancerId)
                .title("Test Meeting")
                .startTime(start)
                .endTime(end)
                .meetingType(MeetingType.VIDEO_CALL)
                .status(status)
                .build();
        return meetingRepository.save(m);
    }

    // ── findAllByUserId ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByUserId – should return meetings where user is CLIENT")
    void findAllByUserId_userIsClient_found() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING, NOW.plusHours(1), NOW.plusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findAllByUserId(CLIENT_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    @DisplayName("findAllByUserId – should return meetings where user is FREELANCER")
    void findAllByUserId_userIsFreelancer_found() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.ACCEPTED, NOW.plusHours(1), NOW.plusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findAllByUserId(FREELANCER_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFreelancerId()).isEqualTo(FREELANCER_ID);
    }

    @Test
    @DisplayName("findAllByUserId – should NOT return meetings where user is neither CLIENT nor FREELANCER")
    void findAllByUserId_unrelatedUser_returnsEmpty() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING, NOW.plusHours(1), NOW.plusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findAllByUserId(OTHER_USER_ID);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserId – should return meetings from both CLIENT and FREELANCER roles")
    void findAllByUserId_userInBothRoles_returnsAll() {
        // Arrange – user is CLIENT in meeting 1, FREELANCER in meeting 2
        save(CLIENT_ID,     OTHER_USER_ID, MeetingStatus.PENDING,  NOW.plusHours(1), NOW.plusHours(2));
        save(OTHER_USER_ID, CLIENT_ID,     MeetingStatus.ACCEPTED, NOW.plusHours(3), NOW.plusHours(4));

        // Act
        List<Meeting> result = meetingRepository.findAllByUserId(CLIENT_ID);

        // Assert
        assertThat(result).hasSize(2);
    }

    // ── findByUserIdAndStatus ─────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserIdAndStatus – should return only meetings matching given status")
    void findByUserIdAndStatus_statusFilter_worksCorrectly() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING,   NOW.plusHours(1), NOW.plusHours(2));
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.ACCEPTED,  NOW.plusHours(3), NOW.plusHours(4));
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.COMPLETED, NOW.plusHours(5), NOW.plusHours(6));

        // Act
        List<Meeting> pending = meetingRepository.findByUserIdAndStatus(CLIENT_ID, MeetingStatus.PENDING);

        // Assert
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getStatus()).isEqualTo(MeetingStatus.PENDING);
    }

    @Test
    @DisplayName("findByUserIdAndStatus – should return empty when no meetings match status for user")
    void findByUserIdAndStatus_noMatch_returnsEmpty() {
        // Arrange – only PENDING meetings exist
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING, NOW.plusHours(1), NOW.plusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findByUserIdAndStatus(CLIENT_ID, MeetingStatus.DECLINED);

        // Assert
        assertThat(result).isEmpty();
    }

    // ── findUpcomingByUserId ──────────────────────────────────────────────────

    @Test
    @DisplayName("findUpcomingByUserId – should return ACCEPTED meetings starting after 'now'")
    void findUpcoming_acceptedFutureMeeting_isReturned() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.ACCEPTED,
                NOW.plusHours(2), NOW.plusHours(3));

        // Act
        List<Meeting> result = meetingRepository.findUpcomingByUserId(CLIENT_ID, NOW);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(MeetingStatus.ACCEPTED);
    }

    @Test
    @DisplayName("findUpcomingByUserId – should NOT return PENDING meetings (only ACCEPTED)")
    void findUpcoming_pendingMeeting_notReturned() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING, NOW.plusHours(1), NOW.plusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findUpcomingByUserId(CLIENT_ID, NOW);

        // Assert – PENDING is not an "upcoming" meeting
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findUpcomingByUserId – should NOT return past ACCEPTED meetings")
    void findUpcoming_pastMeeting_notReturned() {
        // Arrange – meeting already happened (in the past)
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.ACCEPTED,
                NOW.minusHours(3), NOW.minusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findUpcomingByUserId(CLIENT_ID, NOW);

        // Assert
        assertThat(result).isEmpty();
    }

    // ── countByStatusForUser ──────────────────────────────────────────────────

    @Test
    @DisplayName("countByStatusForUser – should return correct counts per status")
    void countByStatus_multipleMeetings_aggregatesCorrectly() {
        // Arrange – 2 PENDING + 1 ACCEPTED
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING,  NOW.plusHours(1), NOW.plusHours(2));
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING,  NOW.plusHours(3), NOW.plusHours(4));
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.ACCEPTED, NOW.plusHours(5), NOW.plusHours(6));

        // Act
        List<Object[]> rows = meetingRepository.countByStatusForUser(CLIENT_ID);

        // Assert – sum of all counts must equal 3
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();
        assertThat(total).isEqualTo(3L);

        // Verify PENDING count specifically
        rows.stream()
                .filter(r -> r[0] == MeetingStatus.PENDING)
                .findFirst()
                .ifPresent(r -> assertThat((Long) r[1]).isEqualTo(2L));
    }

    @Test
    @DisplayName("countByStatusForUser – should return empty list when user has no meetings")
    void countByStatus_noMeetings_returnsEmptyList() {
        // Act
        List<Object[]> rows = meetingRepository.countByStatusForUser(OTHER_USER_ID);

        // Assert
        assertThat(rows).isEmpty();
    }

    // ── findByClientIdOrderByStartTimeDesc ────────────────────────────────────

    @Test
    @DisplayName("findByClientIdOrderByStartTimeDesc – should order by startTime descending")
    void findByClientId_orderedByStartTimeDesc() {
        // Arrange – earlier first in DB, but result should be latest first
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING,
                NOW.plusHours(1), NOW.plusHours(2));
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING,
                NOW.plusHours(5), NOW.plusHours(6));

        // Act
        List<Meeting> result = meetingRepository.findByClientIdOrderByStartTimeDesc(CLIENT_ID);

        // Assert – latest start time comes first
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartTime())
                .isAfter(result.get(1).getStartTime());
    }

    // ── findCompletable ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findCompletable – should return ACCEPTED meetings whose endTime is in the past")
    void findCompletable_pastAcceptedMeeting_isReturned() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.ACCEPTED,
                NOW.minusHours(3), NOW.minusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findCompletable(NOW);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findCompletable – should NOT return PENDING meetings even if they are past")
    void findCompletable_pastPendingMeeting_notReturned() {
        // Arrange
        save(CLIENT_ID, FREELANCER_ID, MeetingStatus.PENDING,
                NOW.minusHours(3), NOW.minusHours(2));

        // Act
        List<Meeting> result = meetingRepository.findCompletable(NOW);

        // Assert
        assertThat(result).isEmpty();
    }
}
