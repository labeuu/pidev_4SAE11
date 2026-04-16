package tn.esprit.project.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.project.Client.SkillClient;
import tn.esprit.project.Dto.request.ProjectRequest;
import tn.esprit.project.Dto.request.NewSkillRequest;
import tn.esprit.project.Dto.response.JointProjectsResponse;
import tn.esprit.project.Dto.response.ProjectResponse;
import tn.esprit.project.Dto.Skills;
import tn.esprit.project.Entities.Enums.ApplicationStatus;
import tn.esprit.project.Entities.Enums.ProjectStatus;
import tn.esprit.project.Entities.Project;
import tn.esprit.project.Entities.ProjectApplication;
import tn.esprit.project.Repository.ProjectApplicationRepository;
import tn.esprit.project.Repository.ProjectRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProjectService.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectApplicationRepository projectApplicationRepository;

    @Mock
    private SkillClient skillClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void addProject_withNoNewSkills_savesAndPublishesEvent() {
        ProjectRequest request = new ProjectRequest();
        request.setClientId(1L);
        request.setTitle("P1");
        request.setSkillIds(new ArrayList<>(List.of(10L, 11L)));
        
        Project savedProject = project(1L, 1L, "P1", ProjectStatus.OPEN);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        Project result = projectService.addProject(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.OPEN);
        verify(skillClient, never()).createSkill(any());
        verify(eventPublisher).publishEvent(any(ProjectService.ProjectCreatedEvent.class));
    }

    @Test
    void addProject_withNewSkills_callsSkillClientAndSaves() {
        ProjectRequest request = new ProjectRequest();
        request.setClientId(1L);
        request.setTitle("P1");
        request.setSkillIds(new ArrayList<>());
        NewSkillRequest newSkill = new NewSkillRequest();
        newSkill.setName("Java");
        newSkill.setDomains(List.of("Backend"));
        request.setNewSkills(List.of(newSkill));

        Skills skillResponse = new Skills();
        skillResponse.setId(100L);
        skillResponse.setName("Java");
        when(skillClient.createSkill(any(Skills.class))).thenReturn(skillResponse);

        Project savedProject = project(1L, 1L, "P1", ProjectStatus.OPEN);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        Project result = projectService.addProject(request);

        assertThat(result).isNotNull();
        verify(skillClient).createSkill(any(Skills.class));
        verify(eventPublisher).publishEvent(any(ProjectService.ProjectCreatedEvent.class));
    }

    @Test
    void addProject_whenSkillClientThrows_savesWithoutNewSkill() {
        ProjectRequest request = new ProjectRequest();
        request.setClientId(1L);
        request.setTitle("P1");
        request.setSkillIds(new ArrayList<>());
        NewSkillRequest ns = new NewSkillRequest();
        ns.setName("Go");
        ns.setDomains(List.of("Backend"));
        request.setNewSkills(List.of(ns));

        when(skillClient.createSkill(any())).thenThrow(new RuntimeException("Skill service down"));
        Project savedProject = project(1L, 1L, "P1", ProjectStatus.OPEN);
        savedProject.setSkillIds(new ArrayList<>());
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        Project result = projectService.addProject(request);

        assertThat(result.getSkillIds()).isEmpty();
        verify(eventPublisher).publishEvent(any(ProjectService.ProjectCreatedEvent.class));
    }

    @Test
    void addProject_clientIdNull_doesNotPublishEvent() {
        ProjectRequest request = new ProjectRequest();
        request.setClientId(null);
        request.setTitle("P1");
        
        Project savedProject = project(1L, null, "P1", ProjectStatus.OPEN);
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

        Project result = projectService.addProject(request);

        assertThat(result).isNotNull();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateProject_noStatusChange_savesNormally() {
        Project existing = project(1L, 1L, "Old Title", ProjectStatus.OPEN);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenReturn(existing);

        Project payload = new Project();
        payload.setId(1L);
        payload.setTitle("New Title");

        Project result = projectService.updateProject(payload);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.OPEN);
        verify(eventPublisher, never()).publishEvent(any(ProjectService.ProjectCompletedEvent.class));
    }

    @Test
    void updateProject_statusChangedToCompleted_publishesEvent() {
        Project existing = project(1L, 1L, "Title", ProjectStatus.IN_PROGRESS);
        ProjectApplication app = application(1L, 2L, ApplicationStatus.ACCEPTED);
        existing.setApplications(List.of(app));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenReturn(existing);
        when(projectApplicationRepository.findByProjectId(1L)).thenReturn(List.of(app));

        Project payload = new Project();
        payload.setId(1L);
        payload.setStatus(ProjectStatus.COMPLETED);

        Project result = projectService.updateProject(payload);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(ProjectService.ProjectCompletedEvent.class));
    }

    @Test
    void updateProject_withNullId_savesDirectly() {
        Project payload = project(null, 2L, "Direct Save", ProjectStatus.OPEN);
        when(projectRepository.save(payload)).thenReturn(payload);

        Project result = projectService.updateProject(payload);

        assertThat(result).isEqualTo(payload);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void completeProject_withAcceptedApplication_changesStatusAndPublishesEvent() {
        Project existing = project(1L, 1L, "Title", ProjectStatus.OPEN);
        ProjectApplication app = application(1L, 2L, ApplicationStatus.ACCEPTED);
        existing.setApplications(List.of(app));
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenReturn(existing);
        when(projectApplicationRepository.findByProjectId(1L)).thenReturn(List.of(app));

        projectService.completeProject(1L);

        assertThat(existing.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(ProjectService.ProjectCompletedEvent.class));
    }

    @Test
    void completeProject_notFound_throwsException() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.completeProject(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");
    }

    @Test
    void deleteProject_nominalCase_deletesApplicationsAndProject() {
        projectService.deleteProject(1L);

        verify(projectApplicationRepository).deleteByProjectId(1L);
        verify(projectRepository).deleteById(1L);
    }

    @Test
    void getProjectById_found_returnsProject() {
        Project p = project(1L, 2L, "Title", ProjectStatus.OPEN);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));

        Project result = projectService.getProjectById(1L);

        assertThat(result).isEqualTo(p);
    }

    @Test
    void getProjectById_notFound_throwsException() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");
    }

    @Test
    void getProjectResponse_withSkills_callsSkillClient() {
        Project payload = project(1L, 1L, "Proj", ProjectStatus.OPEN);
        payload.setSkillIds(List.of(10L, 20L));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(payload));
        
        Skills s1 = new Skills(); s1.setId(10L); s1.setName("S1");
        Skills s2 = new Skills(); s2.setId(20L); s2.setName("S2");
        when(skillClient.getSkillsByIds(List.of(10L, 20L))).thenReturn(List.of(s1, s2));

        ProjectResponse response = projectService.getProjectResponse(1L);

        assertThat(response.getSkills()).hasSize(2);
        verify(skillClient).getSkillsByIds(List.of(10L, 20L));
    }

    @Test
    void getProjectResponse_noSkills_doesNotCallSkillClient() {
        Project payload = project(1L, 1L, "Proj", ProjectStatus.OPEN);
        payload.setSkillIds(new ArrayList<>());
        when(projectRepository.findById(1L)).thenReturn(Optional.of(payload));

        ProjectResponse response = projectService.getProjectResponse(1L);

        assertThat(response.getSkills()).isEmpty();
        verify(skillClient, never()).getSkillsByIds(anyList());
    }

    @Test
    void getAllProjectResponses_nominalCase() {
        Project p1 = project(1L, 2L, "P1", ProjectStatus.OPEN);
        p1.setSkillIds(List.of(10L));
        Project p2 = project(2L, 2L, "P2", ProjectStatus.COMPLETED);
        p2.setSkillIds(new ArrayList<>());
        
        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));
        
        Skills s1 = new Skills(); s1.setId(10L);
        when(skillClient.getSkillsByIds(List.of(10L))).thenReturn(List.of(s1));

        List<ProjectResponse> results = projectService.getAllProjectResponses();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getSkills()).hasSize(1);
    }

    @Test
    void getProjectsByClientId_returnsList() {
        Project p = project(1L, 5L, "P", ProjectStatus.OPEN);
        when(projectRepository.findByClientId(5L)).thenReturn(List.of(p));

        List<Project> results = projectService.getProjectsByClientId(5L);

        assertThat(results).containsExactly(p);
    }

    @Test
    void getJointProjects_nominalCase() {
        Project p = project(1L, 2L, "P", ProjectStatus.OPEN);
        when(projectRepository.findJointProjects(2L, 3L)).thenReturn(List.of(p));

        JointProjectsResponse response = projectService.getJointProjects(2L, 3L);

        assertThat(response.getSharedProjectCount()).isEqualTo(1L);
        assertThat(response.getProjects()).hasSize(1);
    }

    @Test
    void getRecommendedProjects_withMatchingSkills_returnsProject() {
        Skills fp1 = new Skills(); fp1.setId(10L); fp1.setName("Java");
        Skills fp2 = new Skills(); fp2.setId(20L); fp2.setName("Angular");
        when(skillClient.getSkillsByUserId(5L)).thenReturn(List.of(fp1, fp2));

        Project p = project(1L, 2L, "P", ProjectStatus.OPEN);
        p.setSkillIds(List.of(10L));
        when(projectRepository.findByStatus(ProjectStatus.OPEN)).thenReturn(List.of(p));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));

        when(skillClient.getSkillsByIds(List.of(10L))).thenReturn(List.of(fp1));

        List<ProjectResponse> recommended = projectService.getRecommendedProjects(5L);

        assertThat(recommended).hasSize(1);
        assertThat(recommended.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getRecommendedProjects_skillClientThrows_returnsEmptyList() {
        when(skillClient.getSkillsByUserId(5L)).thenThrow(new RuntimeException("Offline"));

        List<ProjectResponse> recommended = projectService.getRecommendedProjects(5L);

        assertThat(recommended).isEmpty();
        verify(projectRepository, never()).findByStatus(any());
    }

    @Test
    void getRecommendedProjects_noFreelanceSkills_returnsEmptyList() {
        when(skillClient.getSkillsByUserId(5L)).thenReturn(List.of());

        List<ProjectResponse> recommended = projectService.getRecommendedProjects(5L);

        assertThat(recommended).isEmpty();
        verify(projectRepository, never()).findByStatus(any());
    }

    @Test
    void getProjectStatistics_returnsMap() {
        Project p1 = project(1L, 2L, "P", ProjectStatus.OPEN);
        Project p2 = project(2L, 2L, "P", ProjectStatus.IN_PROGRESS);
        when(projectRepository.findAll()).thenReturn(List.of(p1, p2));

        Map<String, Object> stats = projectService.getProjectStatistics();

        assertThat(stats).containsEntry("totalProjects", 2L);
        assertThat(stats).containsEntry("openProjects", 1L);
    }

    @Test
    void exportProjectsToPdf_callsFindAllAndReturnsPdfBytes() {
        Project p = project(1L, 2L, "P", ProjectStatus.OPEN);
        p.setBudget(new BigDecimal("100"));
        p.setDeadline(LocalDateTime.now());
        when(projectRepository.findAll()).thenReturn(List.of(p));

        byte[] pdf = projectService.exportProjectsToPdf();

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void countCompletedProjectsByFreelancer_callsRepository() {
        when(projectApplicationRepository.countByFreelanceIdAndStatusAndProject_Status(5L, ApplicationStatus.ACCEPTED, ProjectStatus.COMPLETED)).thenReturn(3L);

        long count = projectService.countCompletedProjectsByFreelancer(5L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void countCreatedProjectsByClient_callsRepository() {
        when(projectRepository.countByClientId(5L)).thenReturn(10L);

        long count = projectService.countCreatedProjectsByClient(5L);

        assertThat(count).isEqualTo(10L);
    }

    private static Project project(Long id, Long clientId, String title, ProjectStatus status) {
        Project p = new Project();
        p.setId(id);
        p.setClientId(clientId);
        p.setTitle(title);
        p.setStatus(status);
        p.setSkillIds(new ArrayList<>());
        p.setApplications(new ArrayList<>());
        return p;
    }

    private static ProjectApplication application(Long id, Long freelanceId, ApplicationStatus status) {
        ProjectApplication app = new ProjectApplication();
        app.setId(id);
        app.setFreelanceId(freelanceId);
        app.setStatus(status);
        return app;
    }
}
