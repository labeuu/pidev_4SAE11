package tn.esprit.gamification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "project-service",
        url = "${project.service.url:http://localhost:8084}", // 🛠 Port corrigé (8084)
        path = "/projects"
)
public interface ProjectClient {

    @GetMapping("/count/completed")
    long countCompletedProjects(@RequestParam("userId") Long userId);

    @GetMapping("/count/created")
    long countCreatedProjects(@RequestParam("userId") Long userId);
}
