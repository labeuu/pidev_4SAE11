package tn.esprit.gamification.Mapper;

import org.springframework.stereotype.Component;
import tn.esprit.gamification.Dto.AchievementDTO;
import tn.esprit.gamification.Entities.Achievement;

@Component
public class AchievementMapper {

    public AchievementDTO toDTO(Achievement a) {
        if (a == null) return null;
        AchievementDTO dto = new AchievementDTO();
        dto.setId(a.getId());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setXpReward(a.getXpReward());
        dto.setConditionType(a.getConditionType());
        dto.setConditionThreshold(a.getConditionThreshold());
        dto.setIconEmoji(a.getIconEmoji());
        dto.setTargetRole(a.getTargetRole());
        return dto;
    }

    public Achievement toEntity(AchievementDTO dto) {
        if (dto == null) return null;
        Achievement a = new Achievement();
        a.setId(dto.getId());
        a.setTitle(dto.getTitle());
        a.setDescription(dto.getDescription());
        a.setXpReward(dto.getXpReward());
        a.setConditionType(dto.getConditionType());
        a.setConditionThreshold(dto.getConditionThreshold());
        a.setIconEmoji(dto.getIconEmoji());
        a.setTargetRole(dto.getTargetRole());
        return a;
    }
}