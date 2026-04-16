package com.esprit.task.client;

import com.esprit.task.dto.ProjectDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "Project", path = "/projects")
public interface ProjectClient {

    @GetMapping("/{id}")
    ProjectDto getProjectById(@PathVariable("id") Long id);

    @GetMapping("/list")
    List<ProjectDto> getProjects();

    @GetMapping("/client/{clientId}")
    List<ProjectDto> getProjectsByClientId(@PathVariable("clientId") Long clientId);
}
