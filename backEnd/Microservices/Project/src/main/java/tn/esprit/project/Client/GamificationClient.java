package tn.esprit.project.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "gamification-service",
        url = "${gamification.service.url:http://localhost:8088}", // 🛠 Port corrigé (8088)
        path = "/api/gamification"
)
public interface GamificationClient {

    @PostMapping("/project-created")
    void handleProjectCreated(@RequestParam("userId") Long userId);

    @PostMapping("/project-completed")
    void handleProjectCompleted(@RequestParam("userId") Long userId);
}
