package com.esprit.task.service;

import com.esprit.task.client.AImodelClient;
import com.esprit.task.client.PlanningClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.client.TaskAiBackend;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.TaskStatsExtendedDto;
import com.esprit.task.dto.ai.AiContextRequest;
import com.esprit.task.dto.ai.AiGenerateResponse;
import com.esprit.task.dto.ai.AiPromptRequest;
import com.esprit.task.dto.ai.AiProposedTaskDto;
import com.esprit.task.dto.ai.TaskAiAskTasksResponse;
import com.esprit.task.dto.ai.TaskAiClientBriefResponse;
import com.esprit.task.dto.ai.TaskAiDefinitionOfDoneResponse;
import com.esprit.task.dto.ai.TaskAiWorkloadCoachResponse;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
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

    @Mock
    private PlanningClient planningClient;

    @Mock
    private TaskService taskService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TaskAiService taskAiService;

    @BeforeEach
    void setUp() {
        taskAiService = new TaskAiService(
                aiModelClient,
                projectClient,
                planningClient,
                taskRepository,
                accessService,
                taskService,
                objectMapper);
    }

    @Test
    void suggestDescription_whenNoAccess_throws403() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "Title", TaskAiBackend.OLLAMA))
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

        assertThat(taskAiService.suggestDescription(1L, 9L, " My task ", TaskAiBackend.OLLAMA).getDescription())
                .isEqualTo("Final body");

        ArgumentCaptor<AiPromptRequest> cap = ArgumentCaptor.forClass(AiPromptRequest.class);
        verify(aiModelClient).generate(cap.capture());
        assertThat(cap.getValue().getPrompt()).contains("My task").contains("Proj");
    }

    @Test
    void suggestDescription_whenProjectNull_throws404() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "T", TaskAiBackend.OLLAMA))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void suggestDescription_whenProjectClientFails_throws502() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenThrow(new RuntimeException("network"));

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "T", TaskAiBackend.OLLAMA))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void suggestDescription_whenAiEmpty_throws502() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "P", null, null));
        when(aiModelClient.generate(any())).thenReturn(new AiGenerateResponse(true, "  "));

        assertThatThrownBy(() -> taskAiService.suggestDescription(1L, 9L, "T", TaskAiBackend.OLLAMA))
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

        List<AiProposedTaskDto> out = taskAiService.proposeProjectTasks(1L, 9L, TaskAiBackend.OLLAMA);

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

        assertThatThrownBy(() -> taskAiService.proposeSubtasks(5L, 9L, TaskAiBackend.OLLAMA))
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

        assertThatThrownBy(() -> taskAiService.proposeSubtasks(5L, 9L, TaskAiBackend.OLLAMA))
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

        List<AiProposedTaskDto> out = taskAiService.proposeSubtasks(5L, 9L, TaskAiBackend.OLLAMA);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getTitle()).isEqualTo("S1");
        assertThat(out.get(0).getSuggestedPriority()).isEqualTo(TaskPriority.URGENT);
    }


    @Test
    void proposeSubtasks_whenTaskMissing_throwsEntityNotFound() {
        when(taskRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskAiService.proposeSubtasks(5L, 9L, TaskAiBackend.OLLAMA))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void proposeProjectTasks_skipsBlankTitlesAndMapsDueDateFromIsoPrefix() {
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "P", null, null));
        when(taskRepository.findByProjectIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        String json = "{\"tasks\":[{\"title\":\"\",\"priority\":\"high\"},{\"title\":\"Ok\",\"priority\":\"high\",\"due_date\":\"2026-05-01T00:00:00\"}]}";
        when(aiModelClient.generateTasks(any())).thenReturn(new AiGenerateResponse(true, json));

        List<AiProposedTaskDto> out = taskAiService.proposeProjectTasks(1L, 9L, TaskAiBackend.OLLAMA);

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

        assertThatThrownBy(() -> taskAiService.proposeProjectTasks(1L, 9L, TaskAiBackend.OLLAMA))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void workloadCoach_returnsHighlightsFromBullets() {
        when(taskService.getExtendedStatsByFreelancer(9L, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenReturn(TaskStatsExtendedDto.builder().totalTasks(2L).build());
        when(taskService.getOverdueTasks(Optional.empty(), Optional.of(9L))).thenReturn(List.of());
        when(taskService.findDueSoon(Optional.empty(), Optional.of(9L), 7)).thenReturn(List.of());
        when(aiModelClient.generate(any(AiPromptRequest.class)))
                .thenReturn(new AiGenerateResponse(true, "Summary\n- First\n- Second"));

        TaskAiWorkloadCoachResponse r = taskAiService.workloadCoach(9L, 7, TaskAiBackend.OLLAMA);

        assertThat(r.getSummaryMarkdown()).contains("Summary");
        assertThat(r.getHighlights()).containsExactly("First", "Second");
        ArgumentCaptor<AiPromptRequest> coachCap = ArgumentCaptor.forClass(AiPromptRequest.class);
        verify(aiModelClient).generate(coachCap.capture());
        assertThat(coachCap.getValue().getMaxOutputTokens()).isEqualTo(480);
    }

    @Test
    void definitionOfDone_parsesCriteriaJson() {
        Task t = new Task();
        t.setId(3L);
        t.setProjectId(1L);
        t.setAssigneeId(9L);
        t.setTitle("Do work");
        t.setDescription("Desc");
        when(taskRepository.findById(3L)).thenReturn(Optional.of(t));
        when(accessService.canFreelancerUseProject(9L, 1L)).thenReturn(true);
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 2L, "P", "pd", null));
        String json = "{\"criteria\":[{\"text\":\"C1\",\"mustHave\":true}],\"assumptions\":[\"A1\"]}";
        when(aiModelClient.generate(any(AiPromptRequest.class))).thenReturn(new AiGenerateResponse(true, json));

        TaskAiDefinitionOfDoneResponse r = taskAiService.definitionOfDone(3L, 9L, TaskAiBackend.OLLAMA);

        assertThat(r.getCriteria()).hasSize(1);
        assertThat(r.getCriteria().get(0).getText()).isEqualTo("C1");
        assertThat(r.getAssumptions()).containsExactly("A1");
    }

    @Test
    void askMyTasks_stripsCitedIdsLine() {
        Task t = new Task();
        t.setId(10L);
        t.setProjectId(1L);
        t.setAssigneeId(9L);
        t.setTitle("T1");
        t.setStatus(TaskStatus.TODO);
        t.setPriority(TaskPriority.MEDIUM);
        when(taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(9L)).thenReturn(List.of(t));
        when(aiModelClient.generate(any(AiPromptRequest.class)))
                .thenReturn(new AiGenerateResponse(true, "You have T1.\nCITED_TASK_IDS: 10"));

        TaskAiAskTasksResponse r = taskAiService.askMyTasks(9L, "What is open?", TaskAiBackend.OLLAMA);

        assertThat(r.getAnswerMarkdown()).doesNotContain("CITED_TASK_IDS");
        assertThat(r.getCitedTaskIds()).containsExactly(10L);
        ArgumentCaptor<AiPromptRequest> askCap = ArgumentCaptor.forClass(AiPromptRequest.class);
        verify(aiModelClient).generate(askCap.capture());
        assertThat(askCap.getValue().getMaxOutputTokens()).isEqualTo(380);
    }

    @Test
    void askMyTasks_replacesEchoedOpenRootTasksJsonWithFallback() {
        Task t = new Task();
        t.setId(4L);
        t.setProjectId(1L);
        t.setAssigneeId(9L);
        t.setTitle("Payment integration");
        t.setStatus(TaskStatus.TODO);
        t.setPriority(TaskPriority.MEDIUM);
        when(taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(9L)).thenReturn(List.of(t));
        String bad = "```json\n{\"openRootTasks\":[{\"id\":4,\"title\":\"Payment integration\",\"dueDate\":null}]}\n```\n"
                + "CITED_TASK_IDS: none";
        when(aiModelClient.generate(any(AiPromptRequest.class))).thenReturn(new AiGenerateResponse(true, bad));

        TaskAiAskTasksResponse r = taskAiService.askMyTasks(9L, "What's open?", TaskAiBackend.OLLAMA);

        assertThat(r.getAnswerMarkdown()).doesNotContain("openRootTasks");
        assertThat(r.getAnswerMarkdown()).doesNotContain("```");
        assertThat(r.getAnswerMarkdown()).contains("Payment integration");
    }

    @Test
    void workloadCoach_replacesEchoedSnapshotJsonWithFallback() {
        when(taskService.getExtendedStatsByFreelancer(9L, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()))
                .thenReturn(TaskStatsExtendedDto.builder().totalTasks(3L).overdueCount(1L).build());
        Task od = new Task();
        od.setId(4L);
        od.setTitle("Late task");
        od.setProjectId(2L);
        when(taskService.getOverdueTasks(Optional.empty(), Optional.of(9L))).thenReturn(List.of(od));
        when(taskService.findDueSoon(Optional.empty(), Optional.of(9L), 7)).thenReturn(List.of());
        String bad = "```json\n"
                + "{\"extendedStats\":{\"totalTasks\":3},\"overdueSample\":[],\"dueSoonSample\":[],\"horizonDays\":7}\n"
                + "```";
        when(aiModelClient.generate(any(AiPromptRequest.class))).thenReturn(new AiGenerateResponse(true, bad));

        TaskAiWorkloadCoachResponse r = taskAiService.workloadCoach(9L, 7, TaskAiBackend.OLLAMA);

        assertThat(r.getSummaryMarkdown()).doesNotContain("extendedStats");
        assertThat(r.getSummaryMarkdown()).doesNotContain("```");
        assertThat(r.getSummaryMarkdown()).contains("assigned tasks");
        assertThat(r.getHighlights()).isNotEmpty();
    }

    @Test
    void clientStatusBrief_whenPlanningFails_setsWarning() {
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 99L, "P", null, null));
        when(taskService.getExtendedStatsByProject(1L)).thenReturn(TaskStatsExtendedDto.builder().totalTasks(1L).build());
        when(planningClient.listProgressUpdatesByProject(1L)).thenThrow(new RuntimeException("planning down"));
        when(aiModelClient.generate(any(AiPromptRequest.class))).thenReturn(new AiGenerateResponse(true, "Brief text"));

        TaskAiClientBriefResponse r = taskAiService.clientStatusBrief(1L, 99L, null, null);

        assertThat(r.getBriefMarkdown()).isEqualTo("Brief text");
        assertThat(r.getPlanningDataWarning()).isNotNull();
    }

    @Test
    void clientStatusBrief_forbiddenWhenWrongClient() {
        when(projectClient.getProjectById(1L)).thenReturn(new ProjectDto(1L, 99L, "P", null, null));

        assertThatThrownBy(() -> taskAiService.clientStatusBrief(1L, 1L, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
