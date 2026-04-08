package tn.esprit.meeting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.meeting.dto.*;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.service.MeetingService;

import java.util.List;

/**
 * All endpoints receive the caller's identity via the X-User-Id header,
 * which is injected by the API Gateway after JWT validation.
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<MeetingResponse> create(
            @RequestHeader("X-User-Id") Long clientId,
            @Valid @RequestBody CreateMeetingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.createMeeting(clientId, req));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<MeetingResponse>> getMyMeetings(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(meetingService.getMeetingsForUser(userId));
    }

    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingResponse> getById(
            @PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getById(meetingId));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<MeetingResponse>> getUpcoming(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(meetingService.getUpcomingMeetings(userId));
    }

    @GetMapping("/by-status")
    public ResponseEntity<List<MeetingResponse>> getByStatus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam MeetingStatus status) {
        return ResponseEntity.ok(meetingService.getMeetingsByStatus(userId, status));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{meetingId}")
    public ResponseEntity<MeetingResponse> update(
            @PathVariable Long meetingId,
            @RequestHeader("X-User-Id") Long requesterId,
            @Valid @RequestBody UpdateMeetingRequest req) {
        return ResponseEntity.ok(meetingService.updateMeeting(meetingId, requesterId, req));
    }

    @PatchMapping("/{meetingId}/status")
    public ResponseEntity<MeetingResponse> updateStatus(
            @PathVariable Long meetingId,
            @RequestHeader("X-User-Id") Long requesterId,
            @Valid @RequestBody StatusUpdateRequest req) {
        return ResponseEntity.ok(meetingService.updateStatus(meetingId, requesterId, req));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long meetingId,
            @RequestHeader("X-User-Id") Long requesterId) {
        meetingService.deleteMeeting(meetingId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
