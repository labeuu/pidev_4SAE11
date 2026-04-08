package tn.esprit.meeting.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.enums.MeetingType;

import java.time.LocalDateTime;

@Entity
@Table(name = "meetings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Participants ──────────────────────────────────────────────────────────

    /** ID of the CLIENT who created/proposed the meeting */
    @Column(nullable = false)
    private Long clientId;

    /** ID of the FREELANCER invited to the meeting */
    @Column(nullable = false)
    private Long freelancerId;

    // ── Meeting details ────────────────────────────────────────────────────────

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String agenda;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MeetingType meetingType = MeetingType.VIDEO_CALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MeetingStatus status = MeetingStatus.PENDING;

    // ── Google Calendar / Meet ────────────────────────────────────────────────

    /** Google Calendar event ID (set after calendar event is created) */
    @Column(length = 500)
    private String googleEventId;

    /** Google Meet join URL (set when status = ACCEPTED and calendar event is created) */
    @Column(length = 1000)
    private String meetLink;

    /** Google Calendar ID the event lives in */
    @Column(length = 500)
    private String calendarId;

    // ── Optional reference to a project or contract ───────────────────────────

    /** Optional: link to a specific project this meeting is about */
    private Long projectId;

    /** Optional: link to a specific contract */
    private Long contractId;

    // ── Cancellation reason ───────────────────────────────────────────────────

    @Column(length = 500)
    private String cancellationReason;

    // ── Audit ────────────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
