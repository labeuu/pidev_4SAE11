package tn.esprit.meeting.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.esprit.meeting.enums.MeetingType;

import java.time.LocalDateTime;

@Data
public class UpdateMeetingRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String agenda;

    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private MeetingType meetingType;
}
