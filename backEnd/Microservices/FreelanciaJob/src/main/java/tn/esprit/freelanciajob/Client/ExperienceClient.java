package tn.esprit.freelanciajob.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.freelanciajob.Dto.ExperienceDto;

import java.util.List;

@FeignClient(name = "PORTFOLIO", contextId = "experienceClient", fallback = ExperienceClientFallback.class)
public interface ExperienceClient {

    @GetMapping("/api/experiences/user/{userId}")
    List<ExperienceDto> getExperiencesByUserId(@PathVariable("userId") Long userId);
}
