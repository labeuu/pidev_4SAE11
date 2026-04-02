package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;

import java.util.List;

@Service
public class GamificationServiceImpl implements GamificationService {

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private UserAchievementService userAchievementService;
    @Autowired
    private UserLevelService UserLevelService;

    @Override
    public void handleProjectCompleted(Long userId) {

        // 🎯 récupérer achievements liés
        List<Achievement> achievements =
                achievementService.getByType(conditionType.PROJECT_COMPLETED);

        for (Achievement a : achievements) {
            userAchievementService.unlockAchievement(userId, a.getId());
        }
    }

    @Override
    public void handleProjectCreated(Long userId) {

        List<Achievement> achievements =
                achievementService.getByType(conditionType.PROJECT_CREATED);

        for (Achievement a : achievements) {
            userAchievementService.unlockAchievement(userId, a.getId());
        }
    }
    @Override
    public void handleFastResponse(Long userId) {
        UserLevelService.incrementFastResponderStreak(userId);
    }
}
