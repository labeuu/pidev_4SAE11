package org.example.subcontracting.coach.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.subcontracting.coach.CoachFeatureCode;
import org.example.subcontracting.coach.WalletTransactionType;

import java.time.Instant;

@Entity
@Table(name = "coach_wallet_transactions", indexes = {
        @Index(name = "idx_cwt_wallet", columnList = "wallet_id"),
        @Index(name = "idx_cwt_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CoachWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "balance_before", nullable = false)
    private Integer balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_used", length = 40)
    private CoachFeatureCode featureUsed;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "performed_by_role", length = 20)
    private String performedByRole;

    @Column(columnDefinition = "JSON")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
