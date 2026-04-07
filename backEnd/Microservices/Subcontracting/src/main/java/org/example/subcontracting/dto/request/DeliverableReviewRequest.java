package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeliverableReviewRequest {
    @NotNull
    private Boolean approved;
    private String reviewNote;
}
