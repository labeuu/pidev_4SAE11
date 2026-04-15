package tn.esprit.gamification.Mapper;

import org.junit.jupiter.api.Test;
import tn.esprit.gamification.Dto.AchievementDTO;
import tn.esprit.gamification.Entities.Achievement;
import tn.esprit.gamification.Entities.Enums.TargetRole;
import tn.esprit.gamification.Entities.Enums.conditionType;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementMapperTest {

    private final AchievementMapper mapper = new AchievementMapper();

    @Test
    void toDTO_nominalCase() {
        Achievement a = new Achievement();
        a.setId(1L);
        a.setTitle("Title");
        a.setDescription("Desc");
        a.setXpReward(100);
        a.setConditionType(conditionType.XP_REACHED);
        a.setConditionThreshold(500);
        a.setIconEmoji("⭐");
        a.setTargetRole(TargetRole.FREELANCER);

        AchievementDTO dto = mapper.toDTO(a);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Title");
        assertThat(dto.getDescription()).isEqualTo("Desc");
        assertThat(dto.getXpReward()).isEqualTo(100);
        assertThat(dto.getConditionType()).isEqualTo(conditionType.XP_REACHED);
        assertThat(dto.getConditionThreshold()).isEqualTo(500);
        assertThat(dto.getIconEmoji()).isEqualTo("⭐");
        assertThat(dto.getTargetRole()).isEqualTo(TargetRole.FREELANCER);
    }

    @Test
    void toDTO_nullInput_returnsNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    @Test
    void toEntity_nominalCase() {
        AchievementDTO dto = new AchievementDTO();
        dto.setId(1L);
        dto.setTitle("Title");
        dto.setDescription("Desc");
        dto.setXpReward(100);
        dto.setConditionType(conditionType.XP_REACHED);
        dto.setConditionThreshold(500);
        dto.setIconEmoji("⭐");
        dto.setTargetRole(TargetRole.FREELANCER);

        Achievement entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getDescription()).isEqualTo("Desc");
        assertThat(entity.getXpReward()).isEqualTo(100);
        assertThat(entity.getConditionType()).isEqualTo(conditionType.XP_REACHED);
        assertThat(entity.getConditionThreshold()).isEqualTo(500);
        assertThat(entity.getIconEmoji()).isEqualTo("⭐");
        assertThat(entity.getTargetRole()).isEqualTo(TargetRole.FREELANCER);
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
