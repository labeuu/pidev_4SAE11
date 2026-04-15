package tn.esprit.meeting.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import tn.esprit.meeting.enums.MeetingType;

import java.time.LocalDateTime;

@Data
public class CreateMeetingRequest {

    @NotNull(message = "Freelancer ID is required")
    private Long freelancerId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String agenda;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private MeetingType meetingType = MeetingType.VIDEO_CALL;

    /** Optional: link meeting to a project */
    private Long projectId;

    /** Optional: link meeting to a contract */
    private Long contractId;
}
