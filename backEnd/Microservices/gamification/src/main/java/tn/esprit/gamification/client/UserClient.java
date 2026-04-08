package tn.esprit.gamification.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service",
        url = "${user.service.url:http://localhost:8090}", // 🛠 Port corrigé (8090)
        path = "/api/users"
)
public interface UserClient {

    @GetMapping("/{id}")
    UserResponseDTO getUserById(@PathVariable("id") Long id);

    @Data
    class UserResponseDTO {
        private Long id;
        private String role;
        private String firstName; // 🆕
        private String lastName; // 🆕
    }
}
