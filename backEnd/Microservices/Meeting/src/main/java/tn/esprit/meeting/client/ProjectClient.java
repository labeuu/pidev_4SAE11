package tn.esprit.meeting.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.meeting.dto.ProjectDto;

import java.util.List;

@FeignClient(name = "project", path = "/projects", fallback = ProjectClientFallback.class)
public interface ProjectClient {

    @GetMapping("/client/{clientId}")
    List<ProjectDto> getProjectsByClient(@PathVariable("clientId") Long clientId);

    /** Projects where this user has an accepted application (freelancer side). */
    @GetMapping("/freelancer/{freelancerId}")
    List<ProjectDto> getProjectsByFreelancer(@PathVariable("freelancerId") Long freelancerId);
}
