package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractorInsightDto {
    private String name;
    private Integer profitabilityScore;
    private Integer riskScore;
    private String note;
}

