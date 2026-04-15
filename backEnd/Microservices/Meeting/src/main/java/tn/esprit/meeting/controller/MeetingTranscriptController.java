package tn.esprit.meeting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.meeting.client.UserClient;
import tn.esprit.meeting.dto.UserDto;
import tn.esprit.meeting.entity.MeetingSummary;
import tn.esprit.meeting.entity.MeetingTranscript;
import tn.esprit.meeting.service.MeetingSummaryService;
import tn.esprit.meeting.service.TranscriptService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingTranscriptController {

    private final TranscriptService transcriptService;
    private final MeetingSummaryService summaryService;
    private final UserClient userClient;

    /** Save or update the calling user's transcript for a meeting. */
    @PostMapping("/{meetingId}/transcript")
    public ResponseEntity<MeetingTranscript> saveTranscript(
            @PathVariable Long meetingId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> body) {

        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String userName;
        try {
            UserDto user = userClient.getUserById(userId);
            userName = user.getFullName();
        } catch (Exception e) {
            userName = "User #" + userId;
        }

        return ResponseEntity.ok(transcriptService.save(meetingId, userId, userName, content));
    }

    /** Get all participant transcripts for a meeting. */
    @GetMapping("/{meetingId}/transcripts")
    public ResponseEntity<List<MeetingTranscript>> getTranscripts(@PathVariable Long meetingId) {
        return ResponseEntity.ok(transcriptService.getByMeetingId(meetingId));
    }

    /** Trigger AI summarization of all transcripts for a meeting. */
    @PostMapping("/{meetingId}/summarize")
    public ResponseEntity<?> summarize(@PathVariable Long meetingId) {
        try {
            return ResponseEntity.ok(summaryService.summarize(meetingId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get the existing AI summary (if already generated). */
    @GetMapping("/{meetingId}/summary")
    public ResponseEntity<MeetingSummary> getSummary(@PathVariable Long meetingId) {
        return summaryService.getSummary(meetingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
