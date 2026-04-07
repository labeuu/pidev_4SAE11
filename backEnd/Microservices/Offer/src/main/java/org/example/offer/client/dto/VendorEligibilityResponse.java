package org.example.offer.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Même structure JSON que le microservice Vendor {@code EligibilityDetailResponse}.
 */
@Data
@NoArgsConstructor
public class VendorEligibilityResponse {
    private boolean eligible;
    private String reasonCode;
    private String message;
}
