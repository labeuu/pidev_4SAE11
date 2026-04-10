package com.esprit.task.controller;

import com.esprit.task.client.TaskAiBackend;
import com.esprit.task.config.GlobalExceptionHandler;
import com.esprit.task.dto.ai.AiProposedTaskDto;
import com.esprit.task.dto.ai.TaskAiAskTasksResponse;
import com.esprit.task.dto.ai.TaskAiClientBriefResponse;
import com.esprit.task.dto.ai.TaskAiDefinitionOfDoneResponse;
import com.esprit.task.dto.ai.TaskAiSuggestDescriptionResponse;
import com.esprit.task.dto.ai.TaskAiWorkloadCoachResponse;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.service.TaskAiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskAiController.class)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "welcome.message=Welcome to Task AI API")
class TaskAiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskAiService taskAiService;

    @Test
    void suggestDescription_returns200() throws Exception {
        when(taskAiService.suggestDescription(1L, 99L, "Fix login", TaskAiBackend.OLLAMA))
                .thenReturn(TaskAiSuggestDescriptionResponse.builder().description("Do X then Y").build());

        mockMvc.perform(post("/api/tasks/ai/suggest-description")
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\":1,\"freelancerId\":99,\"title\":\"Fix login\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Do X then Y"));
    }

    @Test
    void suggestDescription_withGeminiHeader_stillUsesOllamaBackend() throws Exception {
        when(taskAiService.suggestDescription(1L, 99L, "Fix login", TaskAiBackend.OLLAMA))
                .thenReturn(TaskAiSuggestDescriptionResponse.builder().description("Ollama").build());

        mockMvc.perform(post("/api/tasks/ai/suggest-description")
                        .header("X-AI-Backend", "gemini")
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\":1,\"freelancerId\":99,\"title\":\"Fix login\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Ollama"));

        verify(taskAiService).suggestDescription(1L, 99L, "Fix login", TaskAiBackend.OLLAMA);
    }

    @Test
    void suggestDescription_forbidden_returns403() throws Exception {
        when(taskAiService.suggestDescription(anyLong(), anyLong(), eq("T"), any(TaskAiBackend.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed"));

        mockMvc.perform(post("/api/tasks/ai/suggest-description")
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\":1,\"freelancerId\":1,\"title\":\"T\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not allowed"));
    }

    @Test
    void proposeProjectTasks_returnsArray() throws Exception {
        when(taskAiService.proposeProjectTasks(5L, 10L, TaskAiBackend.OLLAMA))
                .thenReturn(List.of(
                        AiProposedTaskDto.builder()
                                .title("A")
                                .description("d")
                                .suggestedPriority(TaskPriority.HIGH)
                                .build()));

        mockMvc.perform(post("/api/tasks/ai/propose-project-tasks")
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\":5,\"freelancerId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[0].suggestedPriority").value("HIGH"));
    }

    @Test
    void proposeSubtasks_returnsArray() throws Exception {
        when(taskAiService.proposeSubtasks(7L, 3L, TaskAiBackend.OLLAMA))
                .thenReturn(List.of(
                        AiProposedTaskDto.builder()
                                .title("Sub")
                                .description("")
                                .suggestedPriority(TaskPriority.LOW)
                                .build()));

        mockMvc.perform(post("/api/tasks/ai/propose-subtasks")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskId\":7,\"freelancerId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Sub"));
    }

    @Test
    void workloadCoach_returns200() throws Exception {
        when(taskAiService.workloadCoach(eq(1L), eq(7), any(TaskAiBackend.class)))
                .thenReturn(TaskAiWorkloadCoachResponse.builder()
                        .summaryMarkdown("Stay focused")
                        .highlights(List.of("A"))
                        .build());

        mockMvc.perform(post("/api/tasks/ai/workload-coach")
                        .contentType(APPLICATION_JSON)
                        .content("{\"freelancerId\":1,\"horizonDays\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaryMarkdown").value("Stay focused"));
    }

    @Test
    void askTasks_returns200() throws Exception {
        when(taskAiService.askMyTasks(eq(2L), eq("open?"), any(TaskAiBackend.class)))
                .thenReturn(TaskAiAskTasksResponse.builder().answerMarkdown("Yes").build());

        mockMvc.perform(post("/api/tasks/ai/ask-tasks")
                        .contentType(APPLICATION_JSON)
                        .content("{\"freelancerId\":2,\"question\":\"open?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerMarkdown").value("Yes"));
    }

    @Test
    void clientStatusBrief_returns200() throws Exception {
        when(taskAiService.clientStatusBrief(eq(9L), eq(99L), isNull(), isNull()))
                .thenReturn(TaskAiClientBriefResponse.builder().briefMarkdown("Hello client").build());

        mockMvc.perform(post("/api/tasks/ai/client-status-brief")
                        .contentType(APPLICATION_JSON)
                        .content("{\"projectId\":9,\"clientUserId\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.briefMarkdown").value("Hello client"));
    }

    @Test
    void definitionOfDone_returns200() throws Exception {
        when(taskAiService.definitionOfDone(eq(3L), eq(5L), any(TaskAiBackend.class)))
                .thenReturn(TaskAiDefinitionOfDoneResponse.builder().criteria(List.of()).assumptions(List.of()).build());

        mockMvc.perform(post("/api/tasks/ai/definition-of-done")
                        .contentType(APPLICATION_JSON)
                        .content("{\"taskId\":3,\"freelancerId\":5}"))
                .andExpect(status().isOk());
    }
}
