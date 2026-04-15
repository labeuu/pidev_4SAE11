package org.example.subcontracting.coach.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WalletResponse {
    private Long userId;
    private Integer balance;
    private String currency;
    private Boolean blocked;
    private Boolean firstFreeUsed;
    private Map<String, RemainingAnalysesResponse> remainingAnalysesByFeature;
}
