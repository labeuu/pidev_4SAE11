package tn.esprit.gamification.Services;

import tn.esprit.gamification.Entities.UserLevel;

import java.util.List;

public interface UserLevelService {
    UserLevel getUserLevel(Long userId);
    void addXp(Long userId, int xp);
    // 🆕
    void incrementFastResponderStreak(Long userId);
    void resetFastResponderStreak(Long userId);
    List<UserLevel> getAllUserLevels();
    List<UserLevel> getCurrentTopFreelancers();
    void setTopFreelancer(Long userId, boolean status);
}
