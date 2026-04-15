package tn.esprit.project.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.project.Entities.Enums.ApplicationStatus;
import tn.esprit.project.Entities.ProjectApplication;
import tn.esprit.project.Repository.ProjectApplicationRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IProjectApplicationServicelmp.
 */
@ExtendWith(MockitoExtension.class)
class IProjectApplicationServicelmpTest {

    @Mock
    private ProjectApplicationRepository projectApplicationRepository;

    @InjectMocks
    private IProjectApplicationServicelmp projectApplicationService;

    @Test
    void addProjectApplication_nominalCase() {
        ProjectApplication app = application(1L, 2L, ApplicationStatus.PENDING);
        app.setProposedPrice(new BigDecimal("100"));
        when(projectApplicationRepository.save(any(ProjectApplication.class))).thenReturn(app);

        ProjectApplication result = projectApplicationService.addProjectApplication(app);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(projectApplicationRepository).save(app);
    }

    @Test
    void updateProjectApplication_found_updatesFieldsAndSaves() {
        ProjectApplication existing = application(1L, 2L, ApplicationStatus.PENDING);
        existing.setProposedPrice(new BigDecimal("100"));
        when(projectApplicationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectApplicationRepository.save(any(ProjectApplication.class))).thenReturn(existing);

        ProjectApplication payload = new ProjectApplication();
        payload.setId(1L);
        payload.setProposedPrice(new BigDecimal("200"));
        payload.setCoverLetter("New Cover Letter");

        ProjectApplication result = projectApplicationService.updateProjectApplication(payload);

        assertThat(result.getProposedPrice()).isEqualTo(new BigDecimal("200"));
        assertThat(result.getCoverLetter()).isEqualTo("New Cover Letter");
        verify(projectApplicationRepository).save(any(ProjectApplication.class));
    }

    @Test
    void updateProjectApplication_notFound_throwsException() {
        ProjectApplication payload = new ProjectApplication();
        payload.setId(999L);
        when(projectApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectApplicationService.updateProjectApplication(payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Application not found");
    }

    @Test
    void updateProjectApplication_nullPayloadFields_preservesExisting() {
        ProjectApplication existing = application(1L, 2L, ApplicationStatus.PENDING);
        existing.setCoverLetter("Old Letter");
        when(projectApplicationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectApplicationRepository.save(any(ProjectApplication.class))).thenReturn(existing);

        ProjectApplication payload = new ProjectApplication();
        payload.setId(1L);
        payload.setCoverLetter(null); // Should not overwrite

        ProjectApplication result = projectApplicationService.updateProjectApplication(payload);

        assertThat(result.getCoverLetter()).isEqualTo("Old Letter");
    }

    @Test
    void updateStatus_found_updatesStatusAndRespondedAt() {
        ProjectApplication existing = application(1L, 2L, ApplicationStatus.PENDING);
        when(projectApplicationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectApplicationRepository.save(any(ProjectApplication.class))).thenReturn(existing);

        ProjectApplication result = projectApplicationService.updateStatus(1L, ApplicationStatus.ACCEPTED);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(result.getRespondedAt()).isNotNull();
    }

    @Test
    void updateStatus_notFound_throwsException() {
        when(projectApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectApplicationService.updateStatus(999L, ApplicationStatus.ACCEPTED))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Application not found");
    }

    @Test
    void deleteProjectApplication_nominalCase() {
        projectApplicationService.deleteProjectApplication(1L);
        verify(projectApplicationRepository).deleteById(1L);
    }

    @Test
    void getProjectApplicationById_found_returnsApp() {
        ProjectApplication app = application(1L, 2L, ApplicationStatus.PENDING);
        when(projectApplicationRepository.findById(1L)).thenReturn(Optional.of(app));

        ProjectApplication result = projectApplicationService.getProjectApplicationById(1L);

        assertThat(result).isEqualTo(app);
    }

    @Test
    void getProjectApplicationById_notFound_returnsNull() {
        when(projectApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        ProjectApplication result = projectApplicationService.getProjectApplicationById(999L);

        assertThat(result).isNull(); // Repository.findById().orElse(null) used here
    }

    @Test
    void getApplicationsByProject_returnsList() {
        ProjectApplication app = application(1L, 2L, ApplicationStatus.PENDING);
        when(projectApplicationRepository.findByProjectId(5L)).thenReturn(List.of(app));

        List<ProjectApplication> results = projectApplicationService.getApplicationsByProject(5L);

        assertThat(results).containsExactly(app);
    }

    @Test
    void getAllProjectApplications_returnsList() {
        ProjectApplication app = application(1L, 2L, ApplicationStatus.PENDING);
        when(projectApplicationRepository.findAll()).thenReturn(List.of(app));

        List<ProjectApplication> results = projectApplicationService.getAllProjectApplications();

        assertThat(results).containsExactly(app);
    }

    @Test
    void getApplicationsByFreelance_returnsList() {
        ProjectApplication app = application(1L, 2L, ApplicationStatus.PENDING);
        when(projectApplicationRepository.findByFreelanceId(2L)).thenReturn(List.of(app));

        List<ProjectApplication> results = projectApplicationService.getApplicationsByFreelance(2L);

        assertThat(results).containsExactly(app);
    }

    @Test
    void getProjectApplicationStatistics_delegatesToRepository() {
        projectApplicationService.getProjectApplicationStatistics();
        verify(projectApplicationRepository).getApplicationsStatistics();
    }

    private static ProjectApplication application(Long id, Long freelanceId, ApplicationStatus status) {
        ProjectApplication app = new ProjectApplication();
        app.setId(id);
        app.setFreelanceId(freelanceId);
        app.setStatus(status);
        return app;
    }
}
