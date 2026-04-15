package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliverableSubmitRequest {

    @NotBlank(message = "Submission URL is required")
    @Size(max = 2048)
    @Pattern(
            regexp = "(?i)^https?://\\S+",
            message = "Submission URL must start with http:// or https://")
    private String submissionUrl;

    @Size(max = 2000, message = "Submission note must not exceed 2000 characters")
    private String submissionNote;
}
