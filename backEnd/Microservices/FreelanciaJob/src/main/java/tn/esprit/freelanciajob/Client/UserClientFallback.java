package tn.esprit.freelanciajob.Client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.freelanciajob.Dto.response.UserDto;

import java.util.Collections;
import java.util.List;

/**
 * Circuit-breaker fallback: if the USER service is unreachable, email
 * sending is skipped gracefully rather than crashing the request.
 */
@Slf4j
@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUserById(Long id) {
        log.warn("UserClient fallback: cannot fetch user {}", id);
        return null;
    }

    @Override
    public List<UserDto> getUsersByRole(String role) {
        log.warn("UserClient fallback: cannot fetch users with role {}", role);
        return Collections.emptyList();
    }
}
