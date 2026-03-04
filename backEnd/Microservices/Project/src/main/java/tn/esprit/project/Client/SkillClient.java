package tn.esprit.project.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import tn.esprit.project.Dto.Skills;


import java.util.List;

@FeignClient(name = "PORTFOLIO")
public interface SkillClient {
    @PostMapping("/api/skills/batch")
    List<Skills> getSkillsByIds(@RequestBody List<Long> ids);

    @GetMapping("/api/skills/user/{userId}")
    List<Skills> getSkillsByUserId(@PathVariable Long userId);
}