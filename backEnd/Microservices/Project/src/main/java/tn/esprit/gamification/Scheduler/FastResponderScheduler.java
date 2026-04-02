package tn.esprit.gamification.Scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Entities.UserLevel;
import tn.esprit.gamification.Services.AchievementService;
import tn.esprit.gamification.Services.UserAchievementService;
import tn.esprit.gamification.Services.UserLevelService;

import java.util.List;

@Component
@Slf4j
public class FastResponderScheduler {

    @Autowired
    private UserLevelService userLevelService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private UserAchievementService userAchievementService;

    private static final int STREAK_REQUIRED = 3; // 3 réponses rapides = badge

    // Chaque nuit à 02:00
    @Scheduled(cron = "0 0 2 * * *")
    public void checkFastResponders() {
        log.info("⚡ Vérification des Fast Responders...");

        // 1️⃣ Récupérer les users avec streak >= 3
        List<UserLevel> fastUsers = userLevelService
                .getAllUserLevels()
                .stream()
                .filter(ul -> ul.getFastResponderStreak() >= STREAK_REQUIRED)
                .toList();

        // 2️⃣ Pour chaque user éligible
        List<Achievement> fastAchievements = achievementService
                .getByType(conditionType.FAST_RESPONDER);

        for (UserLevel user : fastUsers) {
            for (Achievement a : fastAchievements) {
                userAchievementService.unlockAchievement(user.getUserId(), a.getId());
            }

            // 3️⃣ Reset le streak après attribution
            userLevelService.resetFastResponderStreak(user.getUserId());

            log.info("⚡ Badge FAST_RESPONDER attribué à user {} (streak: {})",
                    user.getUserId(), user.getFastResponderStreak());
        }
    }
}
