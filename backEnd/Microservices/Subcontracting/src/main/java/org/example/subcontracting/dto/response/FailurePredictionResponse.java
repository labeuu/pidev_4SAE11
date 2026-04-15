package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailurePredictionResponse {
    private Long subcontractId;
    private Integer failureProbability;
    private String riskLevel;
    private List<String> topDrivers;
    private List<String> mitigationPlan;
    private Double confidence;
    private List<AiDecisionReferenceDto> references;
    private LocalDateTime generatedAt;
}

