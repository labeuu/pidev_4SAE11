package tn.esprit.gamification.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.gamification.Entities.UserLevel;

import java.util.List;
import java.util.Optional;

public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {
    Optional<UserLevel> findByUserId(Long userId);

    // 🆕 Pour trouver le top freelancer actuel
    Optional<UserLevel> findTopByOrderByXpDesc();

    // 🆕 Pour réinitialiser les anciens top freelancers
    List<UserLevel> findByIsTopFreelancerTrue();

    // 🆕 Pour récupérer tous les users avec streak >= seuil
    List<UserLevel> findByFastResponderStreakGreaterThanEqual(int streak);

    // 🆕 Pour le leaderboard avec pagination dynamique
    @Query("SELECT ul FROM UserLevel ul ORDER BY ul.xp DESC")
    List<UserLevel> findLeaderboard(Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE UserLevel ul SET ul.xp = :xp, ul.level = :level WHERE ul.userId = :userId")
    void updateXpAndLevel(@Param("userId") Long userId, @Param("xp") int xp, @Param("level") int level);
}