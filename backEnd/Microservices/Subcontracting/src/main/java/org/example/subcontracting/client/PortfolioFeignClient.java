package org.example.subcontracting.client;

import org.example.subcontracting.client.dto.ExperienceRestDto;
import org.example.subcontracting.client.dto.PortfolioSkillDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "portfolioMs", url = "${service.portfolio.url:http://localhost:8086}")
public interface PortfolioFeignClient {

    @GetMapping("/api/skills/user/{userId}")
    List<PortfolioSkillDto> getSkillsByUserId(@PathVariable("userId") Long userId);

    @GetMapping("/api/experiences/user/{userId}")
    List<ExperienceRestDto> getExperiencesByUserId(@PathVariable("userId") Long userId);
}
