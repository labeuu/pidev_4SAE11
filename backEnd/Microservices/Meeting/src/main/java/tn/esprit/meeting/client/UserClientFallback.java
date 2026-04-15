package tn.esprit.meeting.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.meeting.dto.UserDto;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUserById(Long id) {
        log.warn("[MeetingService] UserClient fallback for id={}", id);
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setFirstName("User");
        dto.setLastName("#" + id);
        return dto;
    }
}
