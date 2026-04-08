package tn.esprit.gamification.Dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import tn.esprit.gamification.Entities.Enums.conditionType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AchievementDTO {
    Long id;
    String title;
    String description;
    int xpReward;
    conditionType conditionType;
    
    // 🆕 Nouveaux champs pour le support dynamique
    int conditionThreshold;
    String iconEmoji;

    tn.esprit.gamification.Entities.Enums.TargetRole targetRole;
}
