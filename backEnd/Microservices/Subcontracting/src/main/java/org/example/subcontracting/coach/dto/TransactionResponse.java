package org.example.subcontracting.coach.dto;

import lombok.Builder;
import lombok.Data;
import org.example.subcontracting.coach.CoachFeatureCode;
import org.example.subcontracting.coach.WalletTransactionType;

import java.time.Instant;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private WalletTransactionType type;
    private Integer amount;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private String reason;
    private CoachFeatureCode featureUsed;
    private String performedByRole;
    private Instant createdAt;
}
