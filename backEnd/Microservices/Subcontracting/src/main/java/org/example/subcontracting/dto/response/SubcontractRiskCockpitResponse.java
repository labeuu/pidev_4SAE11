package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractRiskCockpitResponse {
    private Integer totalRiskScore;
    private String level;
    private String streamedNarrative;
    private List<RiskGaugeDto> gauges;
    private List<RiskRecommendationDto> recommendations;
    private List<RiskAlternativeDto> alternatives;
}
