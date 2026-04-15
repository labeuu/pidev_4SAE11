package org.example.subcontracting.coach.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoachInsightResponse {

    private GlobalRisk globalRisk;
    private List<CauseItem> causes;
    private List<ActionItem> actions;
    private ExpectedImpact expectedImpact;
    private String professionalTip;
    private String urgencyVerdict;
    private String coachSignature;

    /** Métadonnées UX */
    private Boolean free;
    private Integer pointsSpent;
    /** Simulation what-if (analyse avancée) — optionnel */
    private Map<String, Object> whatIf;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GlobalRisk {
        private Integer score;
        private String level;
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CauseItem {
        private String title;
        private String detail;
        private Integer impact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionItem {
        private String title;
        private String detail;
        private String priority;
        private Integer expectedRiskReduction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExpectedImpact {
        private Integer riskReductionIfApplied;
        private Integer newEstimatedScore;
        private String confidenceLevel;
    }
}
