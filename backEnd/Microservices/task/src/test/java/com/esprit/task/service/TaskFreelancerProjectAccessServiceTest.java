package com.esprit.task.service;

import com.esprit.task.client.ProjectApplicationClient;
import com.esprit.task.dto.ProjectApplicationFeignDto;
import com.esprit.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskFreelancerProjectAccessServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectApplicationClient projectApplicationClient;

    @InjectMocks
    private TaskFreelancerProjectAccessService service;

    @Test
    void canFreelancerUseProject_whenIdNull_returnsFalse() {
        assertThat(service.canFreelancerUseProject(null, 1L)).isFalse();
        assertThat(service.canFreelancerUseProject(1L, null)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenTaskAssigneeMatch_returnsTrue() {
        when(taskRepository.existsByProjectIdAndAssigneeId(10L, 5L)).thenReturn(true);

        assertThat(service.canFreelancerUseProject(5L, 10L)).isTrue();
    }

    @Test
    void canFreelancerUseProject_whenAcceptedApplicationForProject_returnsTrue() {
        when(taskRepository.existsByProjectIdAndAssigneeId(10L, 5L)).thenReturn(false);
        ProjectApplicationFeignDto app = new ProjectApplicationFeignDto();
        app.setStatus(" accepted ");
        app.setProject(new ProjectApplicationFeignDto.NestedProject(10L, "P"));
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(List.of(app));

        assertThat(service.canFreelancerUseProject(5L, 10L)).isTrue();
    }

    @Test
    void canFreelancerUseProject_whenStatusNotAccepted_returnsFalse() {
        when(taskRepository.existsByProjectIdAndAssigneeId(10L, 5L)).thenReturn(false);
        ProjectApplicationFeignDto app = new ProjectApplicationFeignDto();
        app.setStatus("PENDING");
        app.setProject(new ProjectApplicationFeignDto.NestedProject(10L, null));
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(List.of(app));

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenAppsNull_returnsFalse() {
        when(taskRepository.existsByProjectIdAndAssigneeId(10L, 5L)).thenReturn(false);
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(null);

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenAppsEmpty_returnsFalse() {
        when(taskRepository.existsByProjectIdAndAssigneeId(10L, 5L)).thenReturn(false);
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(Collections.emptyList());

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenProjectNestedNull_returnsFalse() {
        when(taskRepository.existsByProjectIdAndAssigneeId(10L, 5L)).thenReturn(false);
        ProjectApplicationFeignDto app = new ProjectApplicationFeignDto();
        app.setStatus("ACCEPTED");
        app.setProject(null);
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(List.of(app));

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenClientThrows_returnsFalse() {
        when(taskRepository.existsByProjectIdAndAssigneeId(10L, 5L)).thenReturn(false);
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenThrow(new RuntimeException("feign down"));

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }
}
