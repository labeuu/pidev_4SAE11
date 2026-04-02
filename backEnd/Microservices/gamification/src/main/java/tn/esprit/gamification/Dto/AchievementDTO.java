package tn.esprit.gamification.Dto;

import lombok.*;
import tn.esprit.gamification.Entities.Enums.conditionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {

    private Long id;
    private String title;
    private String description;
    private int xpReward;
    private conditionType conditionType;
}
