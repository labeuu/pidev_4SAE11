package tn.esprit.gamification.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.client.NotificationClient;
import tn.esprit.gamification.Dto.NotificationRequestDto;

import java.util.Map;

/**
 * Pushes in-app notifications via the Notification microservice (cron jobs and similar).
 * Failures are logged only so gamification keeps running if Notification is down.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationNotificationService {

    public static final String TYPE_FAST_RESPONDER = "GAMIFICATION_FAST_RESPONDER";
    public static final String TYPE_TOP_FREELANCER = "GAMIFICATION_TOP_FREELANCER";
    public static final String TYPE_TOP_FREELANCER_REVOKED = "GAMIFICATION_TOP_FREELANCER_REVOKED";

    private final NotificationClient notificationClient;

    public void notifyFastResponderBadge(Long userId, String achievementTitle) {
        if (userId == null) {
            return;
        }
        String uid = String.valueOf(userId);
        try {
            notificationClient.create(NotificationRequestDto.builder()
                    .userId(uid)
                    .title("Fast responder badge")
                    .body(achievementTitle != null && !achievementTitle.isBlank()
                            ? "You earned: " + achievementTitle
                            : "You earned the fast responder achievement.")
                    .type(TYPE_FAST_RESPONDER)
                    .data(Map.of("userId", uid))
                    .build());
        } catch (Exception e) {
            log.warn("Notification service: failed FAST_RESPONDER for user {}: {}", uid, e.getMessage());
        }
    }

    public void notifyTopFreelancerCrowned(Long userId, int xp) {
        if (userId == null) {
            return;
        }
        String uid = String.valueOf(userId);
        try {
            notificationClient.create(NotificationRequestDto.builder()
                    .userId(uid)
                    .title("Top freelancer")
                    .body("You are the current top freelancer with " + xp + " XP. Keep it up!")
                    .type(TYPE_TOP_FREELANCER)
                    .data(Map.of("userId", uid, "xp", String.valueOf(xp)))
                    .build());
        } catch (Exception e) {
            log.warn("Notification service: failed TOP_FREELANCER for user {}: {}", uid, e.getMessage());
        }
    }

    public void notifyTopFreelancerRevoked(Long userId) {
        if (userId == null) {
            return;
        }
        String uid = String.valueOf(userId);
        try {
            notificationClient.create(NotificationRequestDto.builder()
                    .userId(uid)
                    .title("Top freelancer update")
                    .body("Another freelancer is now top for this period. Check Growth & achievements for your rank.")
                    .type(TYPE_TOP_FREELANCER_REVOKED)
                    .data(Map.of("userId", uid))
                    .build());
        } catch (Exception e) {
            log.warn("Notification service: failed TOP_FREELANCER_REVOKED for user {}: {}", uid, e.getMessage());
        }
    }
}
