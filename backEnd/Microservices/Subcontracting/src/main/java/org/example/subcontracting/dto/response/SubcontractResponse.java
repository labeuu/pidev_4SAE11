package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractResponse {

    private Long id;
    private Long mainFreelancerId;
    private String mainFreelancerName;
    private Long subcontractorId;
    private String subcontractorName;
    private Long projectId;
    private String projectTitle;
    private Long contractId;
    private String title;
    private String scope;
    private String category;
    private BigDecimal budget;
    private String currency;
    private String status;
    private LocalDate startDate;
    private LocalDate deadline;
    private String rejectionReason;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime statusChangedAt;
    private int totalDeliverables;
    private int approvedDeliverables;
    private int pendingDeliverables;
    private List<DeliverableResponse> deliverables;
}
