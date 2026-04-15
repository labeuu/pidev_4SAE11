package org.example.subcontracting.coach;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoachWalletLowBalanceScheduler {

    private final CoachWalletService coachWalletService;

    /** Chaque jour à 9h : rappels solde faible non encore signalés. */
    @Scheduled(cron = "0 0 9 * * *")
    public void scanLowBalance() {
        int n = coachWalletService.scanLowBalanceWallets();
        if (n > 0) {
            log.info("[COACH-WALLET] Rappels solde faible envoyés : {} wallet(s)", n);
        }
    }
}
