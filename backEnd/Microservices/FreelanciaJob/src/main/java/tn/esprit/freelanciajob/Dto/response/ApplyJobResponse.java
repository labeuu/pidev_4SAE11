package tn.esprit.freelanciajob.Dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Returned exclusively by POST /job-applications/{jobId}/apply.
 * Extends the standard application fields with the list of uploaded attachments.
 * The existing JobApplicationResponse is not modified so existing endpoints are unaffected.
 */
@Data
public class ApplyJobResponse {
    private Long                   id;
    private Long                   jobId;
    private String                 jobTitle;
    private Long                   freelancerId;
    private String                 proposalMessage;
    private BigDecimal             expectedRate;
    private LocalDate              availabilityStart;
    private String                 status;
    private LocalDateTime          createdAt;
    private List<AttachmentResponse> attachments;
}
