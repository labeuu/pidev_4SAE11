package tn.esprit.project.Services;

import tn.esprit.project.Dto.request.ProjectRequest;
import tn.esprit.project.Dto.response.JointProjectsResponse;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Entities.Project;

import java.util.List;
import java.util.Map;

public interface IProjectService {
    Project addProject(ProjectRequest request);
    Project updateProject(Project project);

    void deleteProject(Long id);

    Project getProjectById(Long id);

    List<Project> getAllProjects();
    List<ProjectResponse> getAllProjectResponses();
    ProjectResponse getProjectResponse(Long id);

    List<Project> getProjectsByClientId(Long clientId);

    /** Projets publiés par ce client avec au moins une candidature acceptée (freelancer retenu). */
    List<Project> getProjectsForClientWithAcceptedFreelancer(Long clientId);

    /** Projets où ce freelancer a une candidature acceptée (travail en cours / mission gagnée). */
    List<Project> getProjectsForFreelancer(Long freelancerId);

    /** Projets du client où le freelancer a candidaté (lien historique pour décision agrément). */
    JointProjectsResponse getJointProjects(Long clientId, Long freelancerId);

    List<ProjectResponse> getRecommendedProjects(Long freelancerId);

    Map<String, Object> getProjectStatistics();
    byte[] exportProjectsToPdf();

    long countCompletedProjectsByFreelancer(Long freelancerId);
    long countCreatedProjectsByClient(Long clientId);
}
