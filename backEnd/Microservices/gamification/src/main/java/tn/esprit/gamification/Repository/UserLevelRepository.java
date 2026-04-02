package tn.esprit.gamification.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}