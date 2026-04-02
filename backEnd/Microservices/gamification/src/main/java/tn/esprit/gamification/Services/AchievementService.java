package tn.esprit.gamification.Services;

import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;

import java.util.List;


public interface AchievementService {
    Achievement create(Achievement achievement);
    List<Achievement> getAll();
    Achievement getById(Long id);
    void delete(Long id);

    List<Achievement> getByType(conditionType type);
}
