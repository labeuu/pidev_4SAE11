package org.example.subcontracting.coach;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.NotificationFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.NotificationRequestDto;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoachNotificationDispatcher {

    private final NotificationFeignClient notificationFeignClient;
    private final UserFeignClient userFeignClient;
    private final CoachWalletProperties walletProperties;

    private String userName(Long userId) {
        try {
            UserRemoteDto u = userFeignClient.getUserById(userId);
            if (u == null) return "User #" + userId;
            String n = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                    + (u.getLastName() != null ? u.getLastName() : "")).trim();
            return n.isBlank() ? "User #" + userId : n;
        } catch (Exception e) {
            return "User #" + userId;
        }
    }

    private void send(Long userId, String type, String title, String body) {
        if (userId == null || userId <= 0) return;
        try {
            notificationFeignClient.sendNotification(NotificationRequestDto.builder()
                    .userId(String.valueOf(userId))
                    .type(type)
                    .title(title)
                    .body(body)
                    .build());
        } catch (Exception e) {
            log.warn("[COACH-NOTIF] Échec envoi type={} user={}: {}", type, userId, e.getMessage());
        }
    }

    public void walletLowBalance(Long userId, int balance) {
        String name = userName(userId);
        send(userId, "WALLET_LOW_BALANCE", "Solde coaching faible",
                "Votre solde coaching est faible (" + balance + " pts). Contactez votre administrateur pour recharger.");
        for (Long adminId : safeAdminIds()) {
            send(adminId, "WALLET_LOW_BALANCE", "Alerte solde coaching",
                    "Alerte : " + name + " (id=" + userId + ") n'a plus que " + balance + " pts de coaching.");
        }
    }

    public void walletEmptyBlocked(Long userId) {
        String name = userName(userId);
        for (Long adminId : safeAdminIds()) {
            send(adminId, "WALLET_EMPTY_BLOCKED", "Compte coaching épuisé",
                    "URGENT : le compte coaching de " + name + " (id=" + userId + ") est épuisé et a été automatiquement bloqué. Action requise.");
        }
    }

    public void walletCredited(Long userId, int amount, int newBalance) {
        send(userId, "WALLET_CREDITED", "Points coaching crédités",
                amount + " points coaching ont été ajoutés à votre compte par l'administrateur. Nouveau solde : " + newBalance + " pts.");
    }

    public void walletUnblocked(Long userId, int newBalance) {
        send(userId, "WALLET_UNBLOCKED", "Accès coaching rétabli",
                "Votre accès coaching a été rétabli. Nouveau solde : " + newBalance + " pts.");
    }

    public void rechargeRequest(Long userId, Integer suggestedPoints, String message) {
        String name = userName(userId);
        String body = name + " (id=" + userId + ") demande une recharge de points coaching."
                + (suggestedPoints != null ? " Montant suggéré : " + suggestedPoints + " pts." : "")
                + (message != null && !message.isBlank() ? " Message : " + message : "");
        for (Long adminId : safeAdminIds()) {
            send(adminId, "RECHARGE_REQUEST", "Demande de recharge coaching", body);
        }
    }

    public void walletBonusWelcome(Long userId, int bonus) {
        send(userId, "WALLET_BONUS_WELCOME", "Bienvenue coaching",
                "Bienvenue ! " + bonus + " points coaching offerts pour démarrer. Utilisez votre analyse gratuite maintenant.");
    }

    private List<Long> safeAdminIds() {
        List<Long> ids = walletProperties.getAdminNotifyUserIds();
        return ids == null ? List.of() : ids.stream().filter(id -> id != null && id > 0).distinct().toList();
    }
}
