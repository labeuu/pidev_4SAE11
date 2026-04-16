package tn.esprit.gamification.Mapper;

import org.junit.jupiter.api.Test;
import tn.esprit.gamification.Dto.UserLevelDTO;
import tn.esprit.gamification.Entities.UserLevel;

import static org.assertj.core.api.Assertions.assertThat;

class UserLevelMapperTest {

    private final UserLevelMapper mapper = new UserLevelMapper();

    @Test
    void toDTO_nominalCase() {
        UserLevel ul = new UserLevel();
        ul.setXp(120);
        ul.setLevel(2);

        UserLevelDTO dto = mapper.toDTO(ul);

        assertThat(dto).isNotNull();
        assertThat(dto.getXp()).isEqualTo(120);
        assertThat(dto.getLevel()).isEqualTo(2);
    }
}
