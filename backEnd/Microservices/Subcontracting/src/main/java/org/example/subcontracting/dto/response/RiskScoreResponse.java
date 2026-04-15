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
public class RiskScoreResponse {
    private Long subcontractId;
    private Integer riskScore;
    private String riskLevel;
    private List<AiDecisionReasonDto> reasons;
    private List<String> suggestions;
    private Double confidence;
    private List<AiDecisionReferenceDto> references;
    private LocalDateTime generatedAt;
}

