package org.example.subcontracting.coach;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "coach.wallet")
public class CoachWalletProperties {

    private int lowBalanceThreshold = 300;
    private int criticalBalanceThreshold = 0;
    private int welcomeBonusPoints = 2000;
    /** IDs utilisateurs (souvent admins) à notifier pour solde faible / recharge / urgence */
    private List<Long> adminNotifyUserIds = new ArrayList<>();
}
