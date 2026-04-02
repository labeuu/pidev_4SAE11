package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.UserAchievement;
import tn.esprit.gamification.Repository.AchievementRepository;
import tn.esprit.gamification.Repository.UserAchievementRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserAchievementServiceImpl implements UserAchievementService {

    @Autowired
    private UserAchievementRepository repo;

    @Autowired
    private AchievementRepository achievementRepo;

    @Autowired
    private UserLevelService userLevelService;

    @Override
    public void unlockAchievement(Long userId, Long achievementId) {

        Achievement a = achievementRepo.findById(achievementId).orElse(null);

        if (a == null) return;

        boolean exists = repo.existsByUserIdAndAchievement(userId, a);

        if (!exists) {
            UserAchievement ua = new UserAchievement();
            ua.setUserId(userId);
            ua.setAchievement(a);
            ua.setUnlockedAt(LocalDateTime.now());

            repo.save(ua);

            // 🔥 add XP
            userLevelService.addXp(userId, a.getXpReward());
        }
    }

    @Override
    public List<UserAchievement> getUserAchievements(Long userId) {
        return repo.findByUserId(userId);
    }
}
