package tn.esprit.freelanciajob.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.esprit.freelanciajob.Dto.Skills;

import java.util.List;

@FeignClient(name = "PORTFOLIO", contextId = "skillClient", fallback = SkillClientFallback.class)
public interface SkillClient {

    @PostMapping("/api/skills/batch")
    List<Skills> getSkillsByIds(@RequestBody List<Long> ids);

    @GetMapping("/api/skills/user/{userId}")
    List<Skills> getSkillsByUserId(@PathVariable("userId") Long userId);
}
