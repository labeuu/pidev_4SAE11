package org.example.subcontracting.coach.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RemainingAnalysesResponse {
    private String featureCode;
    private Integer costPoints;
    private Boolean canAfford;
    private Integer remainingCount;
}
