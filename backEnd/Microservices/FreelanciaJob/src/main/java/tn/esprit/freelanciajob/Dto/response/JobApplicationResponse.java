package tn.esprit.freelanciajob.Dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class JobApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long freelancerId;
    private String proposalMessage;
    private BigDecimal expectedRate;
    private LocalDate availabilityStart;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
