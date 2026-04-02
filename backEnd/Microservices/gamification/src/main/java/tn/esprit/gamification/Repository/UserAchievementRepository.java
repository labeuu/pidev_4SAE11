package tn.esprit.gamification.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.UserAchievement;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    boolean existsByUserIdAndAchievement(Long userId, Achievement achievement);
    List<UserAchievement> findByUserId(Long userId);
}
