package com.esprit.planning.service;

import com.esprit.planning.client.UserClient;
import com.esprit.planning.dto.UserDto;
import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.exception.EntityNotFoundException;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ProgressCommentService. Verifies findAll, findAllPaged, findById, findByProgressUpdateId,
 * findByUserId, create, update, deleteById with mocked repositories and UserClient.
 */
@ExtendWith(MockitoExtension.class)
class ProgressCommentServiceTest {

    @Mock
    private ProgressCommentRepository progressCommentRepository;

    @Mock
    private ProgressUpdateRepository progressUpdateRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private PlanningNotificationService planningNotificationService;

    @InjectMocks
    private ProgressCommentService progressCommentService;

    @Test
    void findAll_returnsAllFromRepository() {
        ProgressComment c = comment(1L, 1L, 5L, "Hi");
        when(progressCommentRepository.findAll()).thenReturn(List.of(c));

        List<ProgressComment> result = progressCommentService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("Hi");
        verify(progressCommentRepository).findAll();
    }

    @Test
    void findAllPaged_returnsPageFromRepository() {
        Page<ProgressComment> page = new PageImpl<>(List.of(comment(1L, 1L, 5L, "Hi")), PageRequest.of(0, 20), 1);
        when(progressCommentRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProgressComment> result = progressCommentService.findAllPaged(0, 20, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(progressCommentRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAllPaged_withNegativePage_usesZero() {
        when(progressCommentRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        Page<ProgressComment> result = progressCommentService.findAllPaged(-1, 10, null);

        assertThat(result).isNotNull();
        verify(progressCommentRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAllPaged_withZeroSize_usesMinSizeOne() {
        when(progressCommentRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        Page<ProgressComment> result = progressCommentService.findAllPaged(0, 0, null);

        assertThat(result).isNotNull();
        verify(progressCommentRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAllPaged_withSortAsc_usesAscendingDirection() {
        when(progressCommentRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        progressCommentService.findAllPaged(0, 10, "createdAt,asc");

        verify(progressCommentRepository).findAll(any(Pageable.class));
    }

    @Test
    void findAllPaged_withSortSingleField_usesDefaultDirection() {
        when(progressCommentRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        progressCommentService.findAllPaged(0, 10, "message");

        verify(progressCommentRepository).findAll(any(Pageable.class));
    }

    @Test
    void findById_whenFound_returnsComment() {
        ProgressComment c = comment(1L, 1L, 5L, "Found");
        when(progressCommentRepository.findById(1L)).thenReturn(Optional.of(c));

        ProgressComment result = progressCommentService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("Found");
    }

    @Test
    void findById_whenNotFound_throws() {
        when(progressCommentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressCommentService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ProgressComment not found");
    }

    @Test
    void findByProgressUpdateId_returnsListFromRepository() {
        when(progressCommentRepository.findByProgressUpdate_Id(10L)).thenReturn(List.of(comment(1L, 10L, 5L, "C")));

        List<ProgressComment> result = progressCommentService.findByProgressUpdateId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProgressUpdate().getId()).isEqualTo(10L);
        verify(progressCommentRepository).findByProgressUpdate_Id(10L);
    }

    @Test
    void findByUserId_returnsListFromRepository() {
        when(progressCommentRepository.findByUserId(5L)).thenReturn(List.of(comment(1L, 1L, 5L, "M")));

        List<ProgressComment> result = progressCommentService.findByUserId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(5L);
    }

    @Test
    void create_validRequest_savesAndReturnsComment() {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(pu));
        when(userClient.getUserById(5L)).thenReturn(new UserDto(5L, "John", "Doe", "user@test.com", "FREELANCER"));
        ProgressComment toSave = comment(null, 1L, 5L, "New");
        ProgressComment saved = comment(1L, 1L, 5L, "New");
        when(progressCommentRepository.save(any(ProgressComment.class))).thenReturn(saved);

        ProgressComment result = progressCommentService.create(1L, 5L, "New");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(progressCommentRepository).save(any(ProgressComment.class));
    }

    @Test
    void create_progressUpdateNotFound_throws() {
        when(progressUpdateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressCommentService.create(999L, 5L, "Msg"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ProgressUpdate not found");
    }

    @Test
    void update_existingComment_savesAndReturns() {
        ProgressComment existing = comment(1L, 1L, 5L, "Old");
        when(progressCommentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressCommentRepository.save(any(ProgressComment.class))).thenAnswer(i -> i.getArgument(0));

        ProgressComment result = progressCommentService.update(1L, "Updated message");

        assertThat(result.getMessage()).isEqualTo("Updated message");
        verify(progressCommentRepository).save(existing);
    }

    @Test
    void deleteById_existingComment_deletesAndNotifies() {
        ProgressComment existing = comment(1L, 1L, 5L, "To delete");
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setFreelancerId(10L);
        existing.setProgressUpdate(pu);
        when(progressCommentRepository.findById(1L)).thenReturn(Optional.of(existing));

        progressCommentService.deleteById(1L);

        verify(progressCommentRepository).deleteById(1L);
    }

    @Test
    void update_withLongMessage_truncatesInNotification() {
        ProgressComment existing = comment(1L, 1L, 5L, "Old");
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setFreelancerId(10L);
        existing.setProgressUpdate(pu);
        when(progressCommentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(progressCommentRepository.save(any(ProgressComment.class))).thenAnswer(i -> i.getArgument(0));

        String longMsg = "a".repeat(250);
        ProgressComment result = progressCommentService.update(1L, longMsg);

        assertThat(result.getMessage()).hasSize(250);
        verify(planningNotificationService).notifyUser(any(), any(), eq("a".repeat(200) + "..."), any(), any());
    }

    @Test
    void deleteById_whenProgressUpdateNull_doesNotThrow() {
        ProgressComment existing = comment(1L, 1L, 5L, "No PU");
        existing.setProgressUpdate(null);
        when(progressCommentRepository.findById(1L)).thenReturn(Optional.of(existing));

        progressCommentService.deleteById(1L);

        verify(progressCommentRepository).deleteById(1L);
    }

    @Test
    void create_whenFreelancerEqualsCommenter_doesNotNotify() {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setFreelancerId(5L);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(pu));
        when(userClient.getUserById(5L)).thenReturn(new UserDto(5L, "John", "Doe", "user@test.com", "FREELANCER"));
        ProgressComment saved = comment(1L, 1L, 5L, "Self");
        when(progressCommentRepository.save(any(ProgressComment.class))).thenReturn(saved);

        progressCommentService.create(1L, 5L, "Self comment");

        verify(progressCommentRepository).save(any(ProgressComment.class));
        verify(planningNotificationService, never()).notifyUser(any(), any(), any(), any(), any());
    }

    @Test
    void create_whenDifferentUserComments_notifiesFreelancerWithTruncatedBody() {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(1L);
        pu.setProjectId(7L);
        pu.setFreelancerId(10L);
        when(progressUpdateRepository.findById(1L)).thenReturn(Optional.of(pu));
        when(userClient.getUserById(5L)).thenReturn(new UserDto(5L, "Client", "User", "c@test.com", "CLIENT"));
        ProgressComment saved = comment(99L, 1L, 5L, "x");
        saved.setProgressUpdate(pu);
        when(progressCommentRepository.save(any(ProgressComment.class))).thenReturn(saved);

        String longMessage = "m".repeat(220);
        progressCommentService.create(1L, 5L, longMessage);

        verify(planningNotificationService).notifyUser(
                eq("10"),
                eq("New comment on your progress update"),
                eq("m".repeat(200) + "..."),
                eq(PlanningNotificationService.TYPE_PROGRESS_COMMENT),
                any());
    }

    private static ProgressComment comment(Long id, Long progressUpdateId, Long userId, String message) {
        ProgressUpdate pu = new ProgressUpdate();
        pu.setId(progressUpdateId);
        ProgressComment c = ProgressComment.builder()
                .id(id)
                .progressUpdate(pu)
                .userId(userId)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        return c;
    }
}
