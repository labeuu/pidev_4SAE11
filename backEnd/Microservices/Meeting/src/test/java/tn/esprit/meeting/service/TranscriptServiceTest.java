package tn.esprit.meeting.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.meeting.entity.MeetingTranscript;
import tn.esprit.meeting.repository.MeetingTranscriptRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TranscriptService}.
 *
 * Key behaviour under test:
 *  - Upsert logic: one transcript per (meetingId, userId) pair.
 *  - Combined transcript formatting with speaker labels.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptService – Unit Tests")
class TranscriptServiceTest {

    @Mock
    private MeetingTranscriptRepository repository;

    @InjectMocks
    private TranscriptService transcriptService;

    // ── Helper ────────────────────────────────────────────────────────────────

    private MeetingTranscript transcript(Long id, Long meetingId, Long userId,
                                         String userName, String content) {
        MeetingTranscript t = new MeetingTranscript();
        t.setId(id);
        t.setMeetingId(meetingId);
        t.setUserId(userId);
        t.setUserName(userName);
        t.setContent(content);
        return t;
    }

    // ── save() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save – should INSERT a new transcript when none exists for (meetingId, userId)")
    void save_noExistingTranscript_persistsNewEntity() {
        // Arrange – no existing transcript found
        when(repository.findByMeetingIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        MeetingTranscript saved = transcript(1L, 10L, 1L, "Alice", "Good session");
        when(repository.save(any(MeetingTranscript.class))).thenReturn(saved);

        // Act
        MeetingTranscript result = transcriptService.save(10L, 1L, "Alice", "Good session");

        // Assert – all fields are set on the new entity
        verify(repository).save(argThat(t ->
                t.getMeetingId().equals(10L)
                && t.getUserId().equals(1L)
                && "Alice".equals(t.getUserName())
                && "Good session".equals(t.getContent())
        ));
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("save – should UPDATE the existing transcript when one already exists for (meetingId, userId)")
    void save_existingTranscript_updatesContent() {
        // Arrange – existing transcript is found
        MeetingTranscript existing = transcript(1L, 10L, 1L, "Alice", "Old text");
        when(repository.findByMeetingIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(existing);

        // Act
        transcriptService.save(10L, 1L, "Alice", "Updated text");

        // Assert – the same entity is updated in-place (upsert, not duplicate)
        verify(repository).save(argThat(t ->
                t.getId().equals(1L)
                && "Updated text".equals(t.getContent())
        ));
    }

    @Test
    @DisplayName("save – should update userName when it changes between saves")
    void save_existingTranscript_updatesUserName() {
        // Arrange
        MeetingTranscript existing = transcript(1L, 10L, 1L, "Old Name", "content");
        when(repository.findByMeetingIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(existing);

        // Act
        transcriptService.save(10L, 1L, "New Name", "content");

        // Assert
        verify(repository).save(argThat(t -> "New Name".equals(t.getUserName())));
    }

    // ── getByMeetingId() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getByMeetingId – should delegate to repository ordered by savedAt ascending")
    void getByMeetingId_delegatesToRepository() {
        // Arrange
        List<MeetingTranscript> expected = List.of(
                transcript(1L, 10L, 1L, "Alice", "First"),
                transcript(2L, 10L, 2L, "Bob",   "Second")
        );
        when(repository.findByMeetingIdOrderBySavedAtAsc(10L)).thenReturn(expected);

        // Act
        List<MeetingTranscript> result = transcriptService.getByMeetingId(10L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserName()).isEqualTo("Alice");
        assertThat(result.get(1).getUserName()).isEqualTo("Bob");
    }

    // ── buildCombinedTranscript() ─────────────────────────────────────────────

    @Test
    @DisplayName("buildCombinedTranscript – should prefix each block with [UserName] label")
    void buildCombined_multipleParticipants_formatsWithLabels() {
        // Arrange
        List<MeetingTranscript> transcripts = List.of(
                transcript(1L, 10L, 1L, "Alice", "I think we should proceed."),
                transcript(2L, 10L, 2L, "Bob",   "Agreed, let's move forward.")
        );
        when(repository.findByMeetingIdOrderBySavedAtAsc(10L)).thenReturn(transcripts);

        // Act
        String combined = transcriptService.buildCombinedTranscript(10L);

        // Assert
        assertThat(combined)
                .contains("[Alice]")
                .contains("I think we should proceed.")
                .contains("[Bob]")
                .contains("Agreed, let's move forward.");
    }

    @Test
    @DisplayName("buildCombinedTranscript – should return empty string when no transcripts exist")
    void buildCombined_noTranscripts_returnsEmptyString() {
        // Arrange
        when(repository.findByMeetingIdOrderBySavedAtAsc(10L)).thenReturn(List.of());

        // Act
        String combined = transcriptService.buildCombinedTranscript(10L);

        // Assert
        assertThat(combined).isEmpty();
    }

    @Test
    @DisplayName("buildCombinedTranscript – output for a single participant is correctly formatted")
    void buildCombined_singleParticipant_formatsCorrectly() {
        // Arrange
        List<MeetingTranscript> transcripts = List.of(
                transcript(1L, 10L, 1L, "Alice", "This is a solo note.")
        );
        when(repository.findByMeetingIdOrderBySavedAtAsc(10L)).thenReturn(transcripts);

        // Act
        String combined = transcriptService.buildCombinedTranscript(10L);

        // Assert – result starts with the label and contains the content
        assertThat(combined).startsWith("[Alice]");
        assertThat(combined).contains("This is a solo note.");
    }
}
