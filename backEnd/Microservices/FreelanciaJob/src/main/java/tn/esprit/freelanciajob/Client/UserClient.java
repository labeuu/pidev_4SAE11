package tn.esprit.freelanciajob.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tn.esprit.freelanciajob.Dto.response.UserDto;

import java.util.List;

/**
 * Feign client for the USER microservice (registered in Eureka as "USER").
 *
 * Expected endpoints on the user service:
 *   GET /users/{id}                    → returns UserDto
 *   GET /users/by-role?role=FREELANCER → returns List<UserDto>
 *
 * Adjust the path values to match what your user service actually exposes.
 */
@FeignClient(name = "USER", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    /**
     * Returns all users with the given role (e.g. "FREELANCER").
     * Used to broadcast "new job posted" emails.
     */
    @GetMapping("/api/users/by-role")
    List<UserDto> getUsersByRole(@RequestParam("role") String role);
}
