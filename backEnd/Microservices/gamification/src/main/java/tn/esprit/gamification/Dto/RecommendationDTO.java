package tn.esprit.gamification.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationDTO {
    private String message;
    private int priority; // 🛡️ 1 = High, 2 = Medium, 3 = Low
}
