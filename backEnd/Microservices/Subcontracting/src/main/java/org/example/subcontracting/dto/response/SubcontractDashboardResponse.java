package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MÉTIER 4 — Dashboard & statistiques.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractDashboardResponse {
    private long totalSubcontracts;
    private Map<String, Long> byStatus;
    private Map<String, Long> byCategory;
    private long totalDeliverables;
    private long approvedDeliverables;
    private long pendingDeliverables;
    private long overdueDeliverables;
    private double avgDeliverablesPerSubcontract;
    private double globalCompletionRate;
    private List<String> alerts;
}
