package tn.esprit.gamification.Mapper;

import org.junit.jupiter.api.Test;
import tn.esprit.gamification.Dto.UserAchievementDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.UserAchievement;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserAchievementMapperTest {

    private final UserAchievementMapper mapper = new UserAchievementMapper();

    @Test
    void toDTO_nominalCase() {
        Achievement a = new Achievement();
        a.setTitle("Badge");
        a.setDescription("Desc");
        a.setXpReward(50);

        LocalDateTime now = LocalDateTime.now();

        UserAchievement ua = new UserAchievement();
        ua.setAchievement(a);
        ua.setUnlockedAt(now);

        UserAchievementDTO dto = mapper.toDTO(ua);

        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isEqualTo("Badge");
        assertThat(dto.getDescription()).isEqualTo("Desc");
        assertThat(dto.getXpReward()).isEqualTo(50);
        assertThat(dto.getUnlockedAt()).isEqualTo(now);
    }
}
