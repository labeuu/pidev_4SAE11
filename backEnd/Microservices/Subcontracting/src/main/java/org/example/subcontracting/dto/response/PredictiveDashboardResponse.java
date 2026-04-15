package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveDashboardResponse {
    private String narrativeSummary;
    private Map<String, Double> successRateByCategory;
    private List<SubcontractorInsightDto> topProfitableSubcontractors;
    private List<SubcontractorInsightDto> topRiskySubcontractors;
    private List<MonthInsightDto> bestMonthsForSubcontracting;
    private List<RiskTrendPointDto> riskTrend;
    private String nextIncidentPrediction;
    private String monthlyReportHint;
    private LocalDateTime generatedAt;
}

