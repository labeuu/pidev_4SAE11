package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MÉTIER 3 — Score de performance d'un sous-traitant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractorScoreResponse {
    private Long subcontractorId;
    private String subcontractorName;
    private int score;
    private String label;
    private long totalSubcontracts;
    private long completedSubcontracts;
    private long cancelledSubcontracts;
    private long totalDeliverables;
    private long approvedDeliverables;
    private long rejectedDeliverables;
    private long overdueDeliverables;
    private double completionRate;
    private double onTimeRate;
    private List<String> breakdown;
}
