package org.example.subcontracting.coach.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class InsightHistoryResponse {
    private Long id;
    private Long userId;
    private Long subcontractId;
    private String featureCode;
    private Boolean free;
    private Integer pointsSpent;
    private JsonNode insightResult;
    private Instant createdAt;
}
