package tn.esprit.meeting.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.meeting.client.UserClient;
import tn.esprit.meeting.dto.CommentRequest;
import tn.esprit.meeting.dto.UserDto;
import tn.esprit.meeting.entity.MeetingComment;
import tn.esprit.meeting.service.MeetingCommentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
public class MeetingCommentController {

    private final MeetingCommentService commentService;
    private final UserClient userClient;

    public MeetingCommentController(MeetingCommentService commentService, UserClient userClient) {
        this.commentService = commentService;
        this.userClient = userClient;
    }

    @GetMapping("/{meetingId}/comments")
    public ResponseEntity<List<MeetingComment>> getComments(@PathVariable Long meetingId) {
        return ResponseEntity.ok(commentService.getByMeetingId(meetingId));
    }

    @PostMapping("/{meetingId}/comments")
    public ResponseEntity<MeetingComment> addComment(
            @PathVariable Long meetingId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CommentRequest req) {

        // Always resolve the name from the user service to avoid "User 1" display issues
        String resolvedName;
        try {
            UserDto user = userClient.getUserById(userId);
            resolvedName = user.getFullName();
        } catch (Exception e) {
            resolvedName = req.getUserName() != null ? req.getUserName() : "User #" + userId;
        }

        MeetingComment comment = new MeetingComment();
        comment.setUserId(userId);
        comment.setUserName(resolvedName);
        comment.setContent(req.getContent());
        return ResponseEntity.ok(commentService.create(meetingId, comment));
    }

    @PutMapping("/comments/{id}")
    public ResponseEntity<MeetingComment> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(commentService.update(id, body.get("content")));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
