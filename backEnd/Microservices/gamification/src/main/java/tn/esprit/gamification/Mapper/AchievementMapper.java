package tn.esprit.gamification.Mapper;

import org.springframework.stereotype.Component;
import tn.esprit.gamification.Dto.AchievementDTO;
import tn.esprit.gamification.Entities.Achievement;

@Component
public class AchievementMapper {

    public AchievementDTO toDTO(Achievement a) {
        AchievementDTO dto = new AchievementDTO();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setXpReward(a.getXpReward());
        dto.setConditionType(a.getConditionType()); // ✅ FIX
        return dto;
    }

    public Achievement toEntity(AchievementDTO dto) {
        Achievement a = new Achievement();
        a.setId(dto.getId());
        a.setTitle(dto.getTitle());
        a.setDescription(dto.getDescription());
        a.setXpReward(dto.getXpReward());
        a.setConditionType(dto.getConditionType()); // ✅ FIX
        return a;
    }
}