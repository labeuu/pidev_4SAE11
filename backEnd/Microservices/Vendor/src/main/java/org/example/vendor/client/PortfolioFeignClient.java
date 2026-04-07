package org.example.vendor.client;

import org.example.vendor.client.dto.SkillRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "portfolioMs", url = "${service.portfolio.url:http://localhost:8086}", path = "/api/skills")
public interface PortfolioFeignClient {

    @GetMapping("/user/{userId}")
    List<SkillRemoteDto> getSkillsByUser(@PathVariable("userId") Long userId);
}
