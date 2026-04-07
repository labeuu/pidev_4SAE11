package com.esprit.task.client;

import com.esprit.task.dto.ProjectApplicationFeignDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "projectApplications", url = "${project.service.url:http://localhost:8084}", path = "/applications", contextId = "projectApplicationClient")
public interface ProjectApplicationClient {

    @GetMapping("/freelance/{freelanceId}")
    List<ProjectApplicationFeignDto> getApplicationsByFreelance(@PathVariable("freelanceId") Long freelanceId);
}
