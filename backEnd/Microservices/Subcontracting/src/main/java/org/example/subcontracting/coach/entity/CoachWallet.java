package org.example.subcontracting.coach.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "coach_wallets", indexes = {
        @Index(name = "idx_coach_wallet_user", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private Integer balance = 0;

    @Column(length = 20)
    @Builder.Default
    private String currency = "COACH_POINTS";

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private Boolean blocked = false;

    @Column(name = "first_free_used", nullable = false)
    @Builder.Default
    private Boolean firstFreeUsed = false;

    @Column(name = "low_balance_alerted", nullable = false)
    @Builder.Default
    private Boolean lowBalanceAlerted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
