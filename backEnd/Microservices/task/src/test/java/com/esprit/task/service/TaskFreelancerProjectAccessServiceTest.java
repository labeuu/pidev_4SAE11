package com.esprit.task.service;

import com.esprit.task.client.ContractClient;
import com.esprit.task.dto.ContractDto;
import com.esprit.task.client.ProjectApplicationClient;
import com.esprit.task.dto.ProjectApplicationFeignDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskFreelancerProjectAccessServiceTest {

    @Mock
    private ProjectApplicationClient projectApplicationClient;

    @Mock
    private ContractClient contractClient;

    @InjectMocks
    private TaskFreelancerProjectAccessService service;

    @Test
    void canFreelancerUseProject_whenIdNull_returnsFalse() {
        assertThat(service.canFreelancerUseProject(null, 1L)).isFalse();
        assertThat(service.canFreelancerUseProject(1L, null)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenAcceptedApplicationForProject_returnsTrue() {
        ProjectApplicationFeignDto app = new ProjectApplicationFeignDto();
        app.setStatus(" accepted ");
        app.setProject(new ProjectApplicationFeignDto.NestedProject(10L, "P"));
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(List.of(app));
        when(contractClient.getContractsByFreelancer(5L)).thenReturn(Collections.emptyList());

        assertThat(service.canFreelancerUseProject(5L, 10L)).isTrue();
    }

    @Test
    void canFreelancerUseProject_whenStatusNotAccepted_returnsFalse() {
        ProjectApplicationFeignDto app = new ProjectApplicationFeignDto();
        app.setStatus("PENDING");
        app.setProject(new ProjectApplicationFeignDto.NestedProject(10L, null));
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(List.of(app));
        when(contractClient.getContractsByFreelancer(5L)).thenReturn(Collections.emptyList());

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenAppsNull_returnsFalse() {
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(null);
        when(contractClient.getContractsByFreelancer(5L)).thenReturn(Collections.emptyList());

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenAppsEmpty_returnsFalse() {
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(Collections.emptyList());
        when(contractClient.getContractsByFreelancer(5L)).thenReturn(Collections.emptyList());

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenProjectNestedNull_returnsFalse() {
        ProjectApplicationFeignDto app = new ProjectApplicationFeignDto();
        app.setStatus("ACCEPTED");
        app.setProject(null);
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(List.of(app));
        when(contractClient.getContractsByFreelancer(5L)).thenReturn(Collections.emptyList());

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void canFreelancerUseProject_whenClientThrows_returnsFalse() {
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenThrow(new RuntimeException("feign down"));
        when(contractClient.getContractsByFreelancer(5L)).thenReturn(Collections.emptyList());

        assertThat(service.canFreelancerUseProject(5L, 10L)).isFalse();
    }

    @Test
    void getAccessibleProjectIdsForFreelancer_unionsAcceptedApplicationsAndContracts() {
        ProjectApplicationFeignDto app = new ProjectApplicationFeignDto();
        app.setStatus("ACCEPTED");
        app.setProject(new ProjectApplicationFeignDto.NestedProject(10L, "P"));
        when(projectApplicationClient.getApplicationsByFreelance(5L)).thenReturn(List.of(app));
        when(contractClient.getContractsByFreelancer(5L)).thenReturn(List.of(
                new ContractDto(2L, 10L, 5L, 99L, "ACTIVE"),
                new ContractDto(3L, 11L, 5L, 99L, "COMPLETED"),
                new ContractDto(4L, 12L, 5L, 99L, "DRAFT")
        ));

        Set<Long> ids = service.getAccessibleProjectIdsForFreelancer(5L);

        assertThat(ids).contains(10L, 11L).doesNotContain(12L);
    }
}
