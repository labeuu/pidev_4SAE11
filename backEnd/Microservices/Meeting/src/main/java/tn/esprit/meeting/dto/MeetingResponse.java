package tn.esprit.meeting.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.enums.MeetingType;

import java.time.LocalDateTime;

@Data
@Builder
public class MeetingResponse {
    private Long id;
    private Long clientId;
    private Long freelancerId;
    private String clientName;
    private String freelancerName;
    private String title;
    private String agenda;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private MeetingType meetingType;
    private MeetingStatus status;
    private String meetLink;
    private String googleEventId;
    private Long projectId;
    private Long contractId;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Whether the viewer can join right now (within 15 min of start, status=ACCEPTED) */
    private boolean canJoinNow;
}
