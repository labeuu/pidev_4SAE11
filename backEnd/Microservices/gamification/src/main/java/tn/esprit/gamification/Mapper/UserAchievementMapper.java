package tn.esprit.gamification.Mapper;

import org.springframework.stereotype.Component;
import tn.esprit.gamification.Dto.UserAchievementDTO;
import tn.esprit.gamification.Entities.UserAchievement;

@Component
public class UserAchievementMapper {

    public UserAchievementDTO toDTO(UserAchievement ua) {
        UserAchievementDTO dto = new UserAchievementDTO();
        dto.setTitle(ua.getAchievement().getTitle());
        dto.setDescription(ua.getAchievement().getDescription());
        dto.setXpReward(ua.getAchievement().getXpReward());
        dto.setUnlockedAt(ua.getUnlockedAt());
        return dto;
    }
}
