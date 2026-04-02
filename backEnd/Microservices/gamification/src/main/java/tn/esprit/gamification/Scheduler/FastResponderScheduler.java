package tn.esprit.gamification.Scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Services.AchievementService;
import tn.esprit.gamification.Services.GamificationNotificationService;
import tn.esprit.gamification.Services.UserAchievementService;
import tn.esprit.gamification.Services.UserLevelService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FastResponderScheduler {

    private final UserLevelService userLevelService;
    private final AchievementService achievementService;
    private final UserAchievementService userAchievementService;
    private final GamificationNotificationService gamificationNotificationService;

    private static final int STREAK_REQUIRED = 3;

    @Scheduled(cron = "*/10 * * * * *")
    public void checkFastResponders() {
        log.info("⚡ Vérification des Fast Responders...");

        List<UserLevel> fastUsers = userLevelService
                .getAllUserLevels()
                .stream()
                .filter(ul -> ul.getFastResponderStreak() >= STREAK_REQUIRED)
                .toList();

        List<Achievement> fastAchievements = achievementService
                .getByType(conditionType.FAST_RESPONDER);

        for (UserLevel user : fastUsers) {
            for (Achievement a : fastAchievements) {
                userAchievementService.unlockAchievement(user.getUserId(), a.getId());
            }

            userLevelService.resetFastResponderStreak(user.getUserId());

            String titleForNotify = fastAchievements.isEmpty()
                    ? null
                    : fastAchievements.get(0).getTitle();
            gamificationNotificationService.notifyFastResponderBadge(user.getUserId(), titleForNotify);

            log.info("⚡ Badge FAST_RESPONDER attribué à user {} (streak: {})",
                    user.getUserId(), user.getFastResponderStreak());
        }
    }
}
