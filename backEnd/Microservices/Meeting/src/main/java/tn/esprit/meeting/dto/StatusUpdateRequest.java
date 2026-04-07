package tn.esprit.meeting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.meeting.enums.MeetingStatus;

@Data
public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private MeetingStatus status;

    /** Required when status = CANCELLED or DECLINED */
    private String reason;
}
