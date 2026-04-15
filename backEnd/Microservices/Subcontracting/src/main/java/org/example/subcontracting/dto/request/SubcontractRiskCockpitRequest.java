package org.example.subcontracting.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SubcontractRiskCockpitRequest {
    private Long mainFreelancerId;
    private Long subcontractorId;
    private Long projectId;
    private Long offerId;
    private BigDecimal budget;
    private String startDate;
    private String deadline;
    private String scope;
    private List<String> requiredSkills;
}
