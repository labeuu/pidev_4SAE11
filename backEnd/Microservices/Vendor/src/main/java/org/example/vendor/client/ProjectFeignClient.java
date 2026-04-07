package org.example.vendor.client;

import org.example.vendor.client.dto.JointProjectsRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "projectMs", url = "${service.project.url:http://localhost:8084}", path = "/projects")
public interface ProjectFeignClient {

    @GetMapping("/joint")
    JointProjectsRemoteDto getJointProjects(
            @RequestParam("clientId") Long clientId,
            @RequestParam("freelancerId") Long freelancerId);
}
