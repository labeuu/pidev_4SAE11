package com.esprit.planning.service;

import com.esprit.planning.client.UserClient;
import com.esprit.planning.dto.UserDto;
import com.esprit.planning.entity.ProgressComment;
import com.esprit.planning.exception.EntityNotFoundException;
import com.esprit.planning.entity.ProgressUpdate;
import com.esprit.planning.repository.ProgressCommentRepository;
import com.esprit.planning.repository.ProgressUpdateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for comments on progress updates: list (full or paged), find by id/progress update/user, create, update, delete.
 * Validates that the progress update and user exist; notifies the freelancer when someone else comments.
 */
@Service
public class ProgressCommentService {

    private final ProgressCommentRepository progressCommentRepository;
    private final ProgressUpdateRepository progressUpdateRepository;
    private final UserClient userClient;
    private final PlanningNotificationService planningNotificationService;

    public ProgressCommentService(ProgressCommentRepository progressCommentRepository,
                                  ProgressUpdateRepository progressUpdateRepository,
                                  UserClient userClient,
                                  PlanningNotificationService planningNotificationService) {
        this.progressCommentRepository = progressCommentRepository;
        this.progressUpdateRepository = progressUpdateRepository;
        this.userClient = userClient;
        this.planningNotificationService = planningNotificationService;
    }

    /** Returns all progress comments. */
    @Transactional(readOnly = true)
    // Finds all.
    public List<ProgressComment> findAll() {
        return progressCommentRepository.findAll();
    }

    /** Returns a paginated list of progress comments with optional sort (e.g. createdAt,desc). */
    @Transactional(readOnly = true)
    // Finds all paged.
    public Page<ProgressComment> findAllPaged(int page, int size, String sort) {
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sortObj);
        return progressCommentRepository.findAll(pageable);
    }

    // Performs parse sort.
    private static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        if (parts.length == 1) {
            return Sort.by(parts[0].trim());
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, parts[0].trim());
    }

    /** Returns a comment by id; throws if not found. */
    @Transactional(readOnly = true)
    // Finds by id.
    public ProgressComment findById(Long id) {
        return progressCommentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ProgressComment", id));
    }

    /** Returns all comments for the given progress update. */
    @Transactional(readOnly = true)
    // Finds by progress update id.
    public List<ProgressComment> findByProgressUpdateId(Long progressUpdateId) {
        return progressCommentRepository.findByProgressUpdate_Id(progressUpdateId);
    }

    /** Returns all comments created by the given user. */
    @Transactional(readOnly = true)
    // Finds by user id.
    public List<ProgressComment> findByUserId(Long userId) {
        return progressCommentRepository.findByUserId(userId);
    }

    /** Creates a comment on a progress update; validates progress update and user exist; notifies freelancer if commenter is someone else. */
    @Transactional
    // Creates this operation.
    public ProgressComment create(Long progressUpdateId, Long userId, String message) {
        ProgressUpdate progressUpdate = progressUpdateRepository.findById(progressUpdateId)
                .orElseThrow(() -> new EntityNotFoundException("ProgressUpdate", progressUpdateId));

        // Optionally validate that the user exists in the User microservice.
        // This will throw if the user-service returns an error (4xx/5xx).
        UserDto user = userClient.getUserById(userId);

        ProgressComment comment = ProgressComment.builder()
                .progressUpdate(progressUpdate)
                .userId(user.getId())
                .message(message)
                .build();
        ProgressComment saved = progressCommentRepository.save(comment);
        // Notify the freelancer who owns the progress update (if someone else commented)
        Long freelancerId = progressUpdate.getFreelancerId();
        if (freelancerId != null && !freelancerId.equals(userId)) {
            String body = message != null && message.length() > 200 ? message.substring(0, 200) + "..." : message;
            planningNotificationService.notifyUser(
                String.valueOf(freelancerId),
                "New comment on your progress update",
                body,
                PlanningNotificationService.TYPE_PROGRESS_COMMENT,
                java.util.Map.of(
                    "progressUpdateId", String.valueOf(progressUpdate.getId()),
                    "commentId", String.valueOf(saved.getId()),
                    "projectId", String.valueOf(progressUpdate.getProjectId())
                ));
        }
        return saved;
    }

    /** Updates the message of an existing comment and notifies the freelancer. */
    @Transactional
    // Updates this operation.
    public ProgressComment update(Long id, String message) {
        ProgressComment existing = findById(id);
        existing.setMessage(message);
        ProgressComment saved = progressCommentRepository.save(existing);
        notifyFreelancerAboutComment(saved.getProgressUpdate(), "A comment on your progress update was edited", message);
        return saved;
    }

    /** Deletes a comment and notifies the freelancer. */
    @Transactional
    // Deletes by id.
    public void deleteById(Long id) {
        ProgressComment existing = findById(id);
        var progressUpdate = existing.getProgressUpdate();
        progressCommentRepository.deleteById(id);
        notifyFreelancerAboutComment(progressUpdate, "A comment was removed from your progress update", null);
    }

    // Performs notify freelancer about comment.
    private void notifyFreelancerAboutComment(ProgressUpdate progressUpdate, String title, String body) {
        if (progressUpdate == null) return;
        Long freelancerId = progressUpdate.getFreelancerId();
        if (freelancerId == null) return;
        String bodyText = body != null && body.length() > 200 ? body.substring(0, 200) + "..." : (body != null ? body : "");
        planningNotificationService.notifyUser(
            String.valueOf(freelancerId),
            title,
            bodyText,
            PlanningNotificationService.TYPE_PROGRESS_COMMENT,
            java.util.Map.of(
                "progressUpdateId", String.valueOf(progressUpdate.getId()),
                "projectId", String.valueOf(progressUpdate.getProjectId())
            ));
    }
}