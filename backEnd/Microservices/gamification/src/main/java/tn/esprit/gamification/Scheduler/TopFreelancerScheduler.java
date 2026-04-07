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
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class
TopFreelancerScheduler {

    private final UserLevelService userLevelService;
    private final AchievementService achievementService;
    private final UserAchievementService userAchievementService;
    private final GamificationNotificationService gamificationNotificationService;

    /** Avoid duplicate "you are top" pushes when the same user stays #1 between runs. */
    private volatile Long lastNotifiedTopUserId;

    @Scheduled(cron = "*/10 * * * * *")
    public void detectAndRewardTopFreelancer() {
        log.info("🏅 Détection du Top Freelancer de la semaine...");

        List<UserLevel> allUsers = userLevelService.getAllUserLevels();
        if (allUsers.isEmpty()) {
            return;
        }

        UserLevel topUser = allUsers.stream()
                .max((a, b) -> Integer.compare(a.getXp(), b.getXp()))
                .orElse(null);

        if (topUser == null) {
            return;
        }

        List<UserLevel> oldTopFreelancers = userLevelService.getCurrentTopFreelancers();
        for (UserLevel old : oldTopFreelancers) {
            if (!old.getUserId().equals(topUser.getUserId())) {
                userLevelService.setTopFreelancer(old.getUserId(), false);
                gamificationNotificationService.notifyTopFreelancerRevoked(old.getUserId());
                log.info("❌ Badge TOP_FREELANCER retiré à user {}", old.getUserId());
            }
        }

        userLevelService.setTopFreelancer(topUser.getUserId(), true);

        List<Achievement> topAchievements = achievementService
                .getByType(conditionType.TOP_FREELANCER);

        for (Achievement a : topAchievements) {
            userAchievementService.unlockAchievement(topUser.getUserId(), a.getId());
        }

        if (!Objects.equals(lastNotifiedTopUserId, topUser.getUserId())) {
            gamificationNotificationService.notifyTopFreelancerCrowned(topUser.getUserId(), topUser.getXp());
            lastNotifiedTopUserId = topUser.getUserId();
        }

        log.info("🏅 User {} est le nouveau Top Freelancer ! ({}XP)",
                topUser.getUserId(), topUser.getXp());
    }
}
