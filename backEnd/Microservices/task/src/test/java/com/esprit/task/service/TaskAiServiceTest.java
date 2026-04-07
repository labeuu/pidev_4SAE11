package com.esprit.task.service;

import com.esprit.task.client.AImodelClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.ai.AiContextRequest;
import com.esprit.task.dto.ai.AiGenerateResponse;
import com.esprit.task.dto.ai.AiPromptRequest;
import com.esprit.task.dto.ai.AiProposedTaskDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAiServiceTest {

    @Mock
    private AImodelClient aiModelClient;

    @Mock
    private ProjectClient projectClient;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskFreelancerProjectAccessService accessService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TaskAiService taskAiService;

    @BeforeEach
    void setUp() {
        taskAiService = new TaskAiService(aiModelClient, projectClient, taskRepository, accessService, objectMapper);
    }

    @Test
    void suggestDescription_whenNoAccess_throws403() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "Title"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void suggestDescription_returnsDescriptionFromModel() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "Proj", "Desc", null));
        when(aiModelClient.generate(any(AiPromptRequest.class)))
                .thenReturn(new AiGenerateResponse(true, "  Final body  "));

        assertThat(taskAiService.suggestDescription(1L, 9L, " My task ").getDescription()).isEqualTo("Final body");

        ArgumentCaptor<AiPromptRequest> cap = ArgumentCaptor.forClass(AiPromptRequest.class);
        verify(aiModelClient).generate(cap.capture());
        assertThat(cap.getValue().getPrompt()).contains("My task").contains("Proj");
    }

    @Test
    void suggestDescription_whenProjectNull_throws404() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "T"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void suggestDescription_whenProjectClientFails_throws502() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenThrow(new RuntimeException("network"));

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "T"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void suggestDescription_whenAiEmpty_throws502() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "P", null, null));
        when(aiModelClient.generate(any())).thenReturn(new AiGenerateResponse(true, "  "));

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "T"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void proposeProjectTasks_parsesTasksArray() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "P", "D", null));
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        String json = "{\"tasks\":[{\"title\":\"A\",\"description\":\"d\",\"priority\":\"low\",\"dueDate\":\"2026-04-10\"}]}";
        when(aiModelClient.generateTasks(any(AiContextRequest.class))).thenReturn(new AiGenerateResponse(true, json));

        List<AiProposedTaskDto> out = taskAiService.proposeProjectTasks(1L, 9L);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getTitle()).isEqualTo("A");
        assertThat(out.get(0).getSuggestedPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(out.get(0).getSuggestedDueDate()).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    void proposeSubtasks_whenNotAssignee_throws403() {
        Task t = new Task();
        t.setId(5L);
        t.setProjectId(1L);
        t.setAssigneeId(1L);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> taskAiService.proposeSubtasks(5L, 9L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void proposeSubtasks_whenNoProjectAccess_throws403() {
        Task t = new Task();
        t.setId(5L);
        t.setProjectId(1L);
        t.setAssigneeId(9L);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(t));
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> taskAiService.proposeSubtasks(5L, 9L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void proposeSubtasks_returnsItems() {
        Task t = new Task();
        t.setId(5L);
        t.setProjectId(1L);
        t.setAssigneeId(9L);
        t.setTitle("Root");
        when(taskRepository.findById(5L)).thenReturn(Optional.of(t));
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        String json = "{\"subtasks\":[{\"title\":\"S1\",\"description\":\"\",\"priority\":\"urgent\"}]}";
        when(aiModelClient.generateSubtasks(any(AiContextRequest.class))).thenReturn(new AiGenerateResponse(true, json));

        List<AiProposedTaskDto> out = taskAiService.proposeSubtasks(5L, 9L);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getTitle()).isEqualTo("S1");
        assertThat(out.get(0).getSuggestedPriority()).isEqualTo(TaskPriority.URGENT);
    }

    @Test
    void proposeSubtasks_whenTaskMissing_throwsEntityNotFound() {
        when(taskRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskAiService.proposeSubtasks(5L, 9L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void proposeProjectTasks_skipsBlankTitlesAndMapsDueDateFromIsoPrefix() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "P", null, null));
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        String json = "{\"tasks\":[{\"title\":\"\",\"priority\":\"high\"},{\"title\":\"Ok\",\"priority\":\"high\",\"due_date\":\"2026-05-01T00:00:00\"}]}";
        when(aiModelClient.generateTasks(any())).thenReturn(new AiGenerateResponse(true, json));

        List<AiProposedTaskDto> out = taskAiService.proposeProjectTasks(1L, 9L);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getTitle()).isEqualTo("Ok");
        assertThat(out.get(0).getSuggestedDueDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(out.get(0).getSuggestedPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void proposeProjectTasks_whenNoUsableItems_throws502() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "P", null, null));
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(aiModelClient.generateTasks(any())).thenReturn(new AiGenerateResponse(true, "{\"tasks\":[]}"));

        assertThatThrownBy(() -> taskAiService.proposeProjectTasks(1L, 9L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
