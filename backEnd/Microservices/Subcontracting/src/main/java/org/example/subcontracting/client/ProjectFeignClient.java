package org.example.subcontracting.client;

import org.example.subcontracting.client.dto.ProjectRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "projectMs", url = "${service.project.url:http://localhost:8084}", path = "/api/projects")
public interface ProjectFeignClient {

    @GetMapping("/{id}")
    ProjectRemoteDto getProjectById(@PathVariable("id") Long id);
}
