package tn.esprit.gamification.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByConditionType(conditionType conditionType);
}
