package tn.esprit.meeting.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.meeting.entity.MeetingComment;
import tn.esprit.meeting.repository.MeetingCommentRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MeetingCommentService}.
 *
 * All repository interactions are mocked; only the service logic is verified.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingCommentService – Unit Tests")
class MeetingCommentServiceTest {

    @Mock
    private MeetingCommentRepository repository;

    @InjectMocks
    private MeetingCommentService commentService;

    // ── Helper ────────────────────────────────────────────────────────────────

    private MeetingComment comment(Long id, Long meetingId, String content) {
        MeetingComment c = new MeetingComment();
        c.setId(id);
        c.setMeetingId(meetingId);
        c.setUserId(100L);
        c.setUserName("Alice");
        c.setContent(content);
        return c;
    }

    // ── getByMeetingId() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getByMeetingId – should delegate to repository and return comments in ascending order")
    void getByMeetingId_delegatesToRepository() {
        // Arrange
        List<MeetingComment> expected = List.of(
                comment(1L, 10L, "First"),
                comment(2L, 10L, "Second")
        );
        when(repository.findByMeetingIdOrderByCreatedAtAsc(10L)).thenReturn(expected);

        // Act
        List<MeetingComment> result = commentService.getByMeetingId(10L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("First");
        assertThat(result.get(1).getContent()).isEqualTo("Second");
        verify(repository).findByMeetingIdOrderByCreatedAtAsc(10L);
    }

    @Test
    @DisplayName("getByMeetingId – should return empty list when no comments exist")
    void getByMeetingId_noComments_returnsEmptyList() {
        // Arrange
        when(repository.findByMeetingIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

        // Act
        List<MeetingComment> result = commentService.getByMeetingId(10L);

        // Assert
        assertThat(result).isEmpty();
    }

    // ── create() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create – should set meetingId on comment before saving")
    void create_setsMeetingIdThenSaves() {
        // Arrange
        MeetingComment incoming = comment(null, null, "Great meeting!"); // meetingId not set yet
        MeetingComment persisted = comment(1L, 10L, "Great meeting!");
        when(repository.save(any(MeetingComment.class))).thenReturn(persisted);

        // Act
        MeetingComment result = commentService.create(10L, incoming);

        // Assert – the repository received a comment with the correct meetingId
        verify(repository).save(argThat(c -> Long.valueOf(10L).equals(c.getMeetingId())));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMeetingId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("create – should return the entity returned by the repository")
    void create_returnsPersistedEntity() {
        // Arrange
        MeetingComment incoming = comment(null, null, "Hello!");
        MeetingComment persisted = comment(5L, 20L, "Hello!");
        when(repository.save(any())).thenReturn(persisted);

        // Act
        MeetingComment result = commentService.create(20L, incoming);

        // Assert
        assertThat(result.getId()).isEqualTo(5L);
    }

    // ── update() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update – should replace content and save the updated comment")
    void update_existingComment_contentIsReplaced() {
        // Arrange
        MeetingComment existing = comment(1L, 10L, "Old content");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(existing);

        // Act
        commentService.update(1L, "New content");

        // Assert
        verify(repository).save(argThat(c -> "New content".equals(c.getContent())));
    }

    @Test
    @DisplayName("update – should throw RuntimeException when comment id does not exist")
    void update_unknownId_throwsRuntimeException() {
        // Arrange
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> commentService.update(99L, "content"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(repository, never()).save(any());
    }

    // ── delete() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete – should delegate to repository.deleteById with the given id")
    void delete_callsRepositoryDeleteById() {
        // Act
        commentService.delete(5L);

        // Assert
        verify(repository).deleteById(5L);
    }
}
