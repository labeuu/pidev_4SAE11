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
public class TopFreelancerScheduler {

    @Autowired
    private UserLevelService userLevelService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private UserAchievementService userAchievementService;

    // Chaque Lundi à 00:00
    @Scheduled(cron = "0 0 0 * * MON")
    public void detectAndRewardTopFreelancer() {
        log.info("🏅 Détection du Top Freelancer de la semaine...");

        // 1️⃣ Récupérer tous les users
        List<UserLevel> allUsers = userLevelService.getAllUserLevels();
        if (allUsers.isEmpty()) return;

        // 2️⃣ Trouver le user avec le plus d'XP
        UserLevel topUser = allUsers.stream()
                .max((a, b) -> Integer.compare(a.getXp(), b.getXp()))
                .orElse(null);

        if (topUser == null) return;

        // 3️⃣ Révoquer l'ancien top freelancer
        List<UserLevel> oldTopFreelancers = userLevelService.getCurrentTopFreelancers();
        for (UserLevel old : oldTopFreelancers) {
            if (!old.getUserId().equals(topUser.getUserId())) {
                userLevelService.setTopFreelancer(old.getUserId(), false);
                log.info("❌ Badge TOP_FREELANCER retiré à user {}", old.getUserId());
            }
        }

        // 4️⃣ Attribuer le badge au nouveau top
        userLevelService.setTopFreelancer(topUser.getUserId(), true);

        // 5️⃣ Débloquer l'achievement TOP_FREELANCER
        List<Achievement> topAchievements = achievementService
                .getByType(conditionType.TOP_FREELANCER);

        for (Achievement a : topAchievements) {
            userAchievementService.unlockAchievement(topUser.getUserId(), a.getId());
        }

        log.info("🏅 User {} est le nouveau Top Freelancer ! ({}XP)",
                topUser.getUserId(), topUser.getXp());
    }
}
