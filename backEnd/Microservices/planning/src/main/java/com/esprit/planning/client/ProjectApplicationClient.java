package com.esprit.planning.client;

import com.esprit.planning.dto.ProjectApplicationFeignDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "projectApplicationsPlanning", url = "${project.service.url:http://localhost:8084}", path = "/applications")
public interface ProjectApplicationClient {
    @GetMapping("/freelance/{freelanceId}")
    List<ProjectApplicationFeignDto> getApplicationsByFreelance(@PathVariable("freelanceId") Long freelanceId);
}
