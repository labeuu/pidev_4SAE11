package tn.esprit.gamification.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.conditionType;
import tn.esprit.gamification.Repository.AchievementRepository;

import java.util.List;

@Service
public class AchievementServiceImpl implements AchievementService {

    @Autowired
    private AchievementRepository repo;

    @Override
    public Achievement create(Achievement a) {
        return repo.save(a);
    }

    @Override
    public List<Achievement> getAll() {
        return repo.findAll();
    }

    @Override
    public Achievement getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    public Achievement update(Long id, Achievement a) {
        Achievement existing = repo.findById(id).orElse(null);
        if (existing == null) return null;
        
        existing.setTitle(a.getTitle());
        existing.setDescription(a.getDescription());
        existing.setXpReward(a.getXpReward());
        existing.setConditionType(a.getConditionType());
        existing.setConditionThreshold(a.getConditionThreshold());
        existing.setIconEmoji(a.getIconEmoji());
        
        return repo.save(existing);
    }

    @Override
    public List<Achievement> getByType(conditionType type) {
        return repo.findByConditionType(type);
    }
}
