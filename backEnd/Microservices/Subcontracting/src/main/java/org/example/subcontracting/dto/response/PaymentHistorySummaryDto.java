package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistorySummaryDto {
    private int pastMissionsConsidered;
    private int missionsWithLateDeliverables;
    private int cancelledOrRejectedMissions;
    /** Placeholder tant qu'aucun module paiement n'est branché */
    private int refundLikeEvents;
    private List<MissionFinanceRowDto> recentMissions;
}
