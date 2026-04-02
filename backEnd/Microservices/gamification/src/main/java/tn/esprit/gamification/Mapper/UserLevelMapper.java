package tn.esprit.gamification.Mapper;

import org.springframework.stereotype.Component;
import tn.esprit.gamification.Dto.UserLevelDTO;
import tn.esprit.gamification.Entities.UserLevel;

@Component
public class UserLevelMapper {

    public UserLevelDTO toDTO(UserLevel ul) {
        UserLevelDTO dto = new UserLevelDTO();
        dto.setXp(ul.getXp());
        dto.setLevel(ul.getLevel());
        return dto;
    }
}
