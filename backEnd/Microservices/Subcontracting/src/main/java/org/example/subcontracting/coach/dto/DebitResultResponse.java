package org.example.subcontracting.coach.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DebitResultResponse {
    private boolean success;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private Integer pointsSpent;
    private Map<String, RemainingAnalysesResponse> remainingAnalyses;
    private boolean lowBalance;
    private boolean blocked;
}
