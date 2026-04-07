package tn.esprit.freelanciajob.Dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JobApplicationRequest {

    @NotNull(message = "jobId is required")
    private Long jobId;

    @NotNull(message = "freelancerId is required")
    private Long freelancerId;

    @NotBlank(message = "proposalMessage is required")
    @Size(min = 20, message = "proposalMessage must be at least 20 characters")
    private String proposalMessage;

    @DecimalMin(value = "0.0", inclusive = false, message = "expectedRate must be positive")
    private BigDecimal expectedRate;

    private LocalDate availabilityStart;
}
