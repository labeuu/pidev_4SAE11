package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcontractFinancialAnalysisResponse {

    private Integer rentabilityScore;
    private FinancialVerdict verdict;
    /** Marge bénéficiaire estimée pour le principal (%) après cette sous-traitance */
    private Double marginRate;
    private Double estimatedRoi;
    /** Seuil ou indicateur de rentabilité (ex. part max du contrat à déléguer, %) */
    private Double breakEvenThreshold;

    private List<String> recommendations;

    private BigDecimal principalContractBudget;
    private BigDecimal subcontractBudget;
    /** Marge restante pour le freelancer principal après engagement des sous-traitances sur ce contrat */
    private BigDecimal remainingMarginForPrincipal;
    /** Part du budget principal allouée à cette sous-traitance (%) */
    private Double subcontractToPrincipalRatioPercent;

    private String currency;

    private PaymentHistorySummaryDto paymentHistorySummary;
    private List<FinancialTimelineEntryDto> financialTimeline;

    /** Budgets des autres sous-traitances actives sur le même contrat (hors cette fiche) */
    private BigDecimal otherSubcontractsOnContractTotal;
}
