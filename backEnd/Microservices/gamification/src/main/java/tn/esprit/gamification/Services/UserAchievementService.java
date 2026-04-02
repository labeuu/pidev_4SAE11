package tn.esprit.gamification.Services;

import tn.esprit.gamification.Entities.UserAchievement;

import java.util.List;

public interface UserAchievementService {
    void unlockAchievement(Long userId, Long achievementId);
    List<UserAchievement> getUserAchievements(Long userId);
}
