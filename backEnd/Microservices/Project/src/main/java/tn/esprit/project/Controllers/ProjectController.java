package tn.esprit.project.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.project.Dto.response.JointProjectsResponse;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Services.IProjectService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;
    private ProjectResponse projectResponse;

    @Value("${welcome.message}")
    private String welcomeMessage;

    @GetMapping("/welcome")
    public String welcome() {
        return welcomeMessage;
    }

    @PostMapping("/add")
    public Project addProject(@jakarta.validation.Valid @RequestBody tn.esprit.project.Dto.request.ProjectRequest request) {
        return projectService.addProject(request);
    }

    @PutMapping("/update")
    public Project updateProject(@RequestBody Project project) {
        return projectService.updateProject(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping("/list")
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjectResponses();
    }

    @GetMapping("/client/{clientId}")
    public List<Project> getProjectsByClientId(@PathVariable Long clientId) {
        return projectService.getProjectsByClientId(clientId);
    }

    /** Projets communs : offres du client pour lesquelles le freelancer a postulé. */
    @GetMapping("/joint")
    public JointProjectsResponse getJointProjects(
            @RequestParam Long clientId,
            @RequestParam Long freelancerId) {
        return projectService.getJointProjects(clientId, freelancerId);
    }

    /** Single project by id (numeric only, so /client/14 is not matched here). */
    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectResponse(id);
    }

    @GetMapping("/recommended")
    public List<ProjectResponse> getRecommendedProjects(
            @RequestParam Long userId) {

        return projectService.getRecommendedProjects(userId);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return projectService.getProjectStatistics();
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportProjectsPdf() {

        byte[] pdf = projectService.exportProjectsToPdf();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=projects.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/count/completed")
    public long countCompletedProjects(@RequestParam Long userId) {
        return projectService.countCompletedProjectsByFreelancer(userId);
    }

    @GetMapping("/count/created")
    public long countCreatedProjects(@RequestParam Long userId) {
        return projectService.countCreatedProjectsByClient(userId);
    }
}
