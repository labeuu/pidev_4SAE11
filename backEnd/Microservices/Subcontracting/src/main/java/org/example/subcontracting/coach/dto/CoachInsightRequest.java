package org.example.subcontracting.coach.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.subcontracting.coach.CoachFeatureCode;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachInsightRequest {

    private Long subcontractId;
    private String scope;
    private String category;
    private BigDecimal budget;
    private Integer durationDays;
    private Long subcontractorId;
    /** Pour analyses payantes ; défaut RISK_DEEP_ANALYSIS côté service si null */
    private CoachFeatureCode featureCode;
}
