package org.example.subcontracting.dto.request;

import lombok.Data;

@Data
public class SubcontractRiskConfirmRequest {
    private Integer totalRiskScore;
    private String selectedAlternativeLabel;
    private String summary;
}
