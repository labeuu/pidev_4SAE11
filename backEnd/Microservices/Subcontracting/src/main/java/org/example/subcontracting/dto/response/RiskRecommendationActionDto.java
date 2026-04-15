package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskRecommendationActionDto {
    private String type;
    private Double budgetMultiplier;
    private Integer durationDeltaDays;
    private Long suggestedSubcontractorId;
    private String scopeHint;
}
