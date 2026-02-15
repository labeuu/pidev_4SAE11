package org.example.offer.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferApplicationRequest {

    @NotNull(message = "Offer ID is required")
    private Long offerId;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotBlank(message = "Message is required")
    @Size(min = 20, max = 2000, message = "Message must be between 20 and 2000 characters")
    private String message;

    @NotNull(message = "Proposed budget is required")
    @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
    private BigDecimal proposedBudget;

    private String portfolioUrl;

    private String attachmentUrl;

    @Min(value = 1, message = "Estimated duration must be at least 1 day")
    private Integer estimatedDuration;
}