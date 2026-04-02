package tn.esprit.gamification.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLevelDTO {
    private int xp;
    private int level;
}
