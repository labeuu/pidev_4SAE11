package tn.esprit.meeting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import tn.esprit.meeting.client.ContractClient;
import tn.esprit.meeting.client.ProjectClient;
import tn.esprit.meeting.dto.*;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.service.MeetingService;

import java.util.ArrayList;
import java.util.List;

/**
 * All endpoints receive the caller's identity via the X-User-Id header,
 * which is injected by the API Gateway after JWT validation.
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;
    private final ProjectClient projectClient;
    private final ContractClient contractClient;

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

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(meetingService.getMeetingStats(userId));
    }

    @GetMapping("/my-projects")
    public ResponseEntity<List<ProjectDto>> getMyProjects(
            @RequestHeader("X-User-Id") Long userId) {
        try {
            return ResponseEntity.ok(projectClient.getProjectsByClient(userId));
        } catch (Exception e) {
            log.error("[MeetingService] Failed to fetch projects for userId={}: {}", userId, e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/my-contracts")
    public ResponseEntity<List<ContractDto>> getMyContracts(
            @RequestHeader("X-User-Id") Long userId) {
        List<ContractDto> result = new ArrayList<>();
        try {
            result.addAll(contractClient.getContractsByClient(userId));
        } catch (Exception e) {
            log.error("[MeetingService] Failed to fetch contracts (client) for userId={}: {}", userId, e.getMessage());
        }
        try {
            result.addAll(contractClient.getContractsByFreelancer(userId));
        } catch (Exception e) {
            log.error("[MeetingService] Failed to fetch contracts (freelancer) for userId={}: {}", userId, e.getMessage());
        }
        return ResponseEntity.ok(result);
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
