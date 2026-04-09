package com.esprit.task.controller;

import com.esprit.task.client.TaskAiBackend;
import com.esprit.task.dto.ai.AiProposedTaskDto;
import com.esprit.task.dto.ai.TaskAiProjectContextRequest;
import com.esprit.task.dto.ai.TaskAiSubtasksRequest;
import com.esprit.task.dto.ai.TaskAiSuggestDescriptionRequest;
import com.esprit.task.dto.ai.TaskAiSuggestDescriptionResponse;
import com.esprit.task.service.TaskAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/ai")
@RequiredArgsConstructor
@Tag(name = "Task AI", description = "AI-assisted task descriptions and proposals (freelancer flows)")
public class TaskAiController {

    private final TaskAiService taskAiService;

    @PostMapping("/suggest-description")
    @Operation(summary = "Suggest task description from title and project context")
    public ResponseEntity<TaskAiSuggestDescriptionResponse> suggestDescription(
            @RequestHeader(value = "X-AI-Backend", required = false) String aiBackendHeader,
            @Valid @RequestBody TaskAiSuggestDescriptionRequest request) {
        TaskAiBackend backend = TaskAiBackend.fromHeader(aiBackendHeader);
        TaskAiSuggestDescriptionResponse body = taskAiService.suggestDescription(
                request.getProjectId(),
                request.getFreelancerId(),
                request.getTitle(),
                backend);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/propose-project-tasks")
    @Operation(summary = "Propose new top-level tasks for a project")
    public ResponseEntity<List<AiProposedTaskDto>> proposeProjectTasks(
            @RequestHeader(value = "X-AI-Backend", required = false) String aiBackendHeader,
            @Valid @RequestBody TaskAiProjectContextRequest request) {
        TaskAiBackend backend = TaskAiBackend.fromHeader(aiBackendHeader);
        List<AiProposedTaskDto> body = taskAiService.proposeProjectTasks(
                request.getProjectId(),
                request.getFreelancerId(),
                backend);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/propose-subtasks")
    @Operation(summary = "Propose subtasks for a root task")
    public ResponseEntity<List<AiProposedTaskDto>> proposeSubtasks(
            @RequestHeader(value = "X-AI-Backend", required = false) String aiBackendHeader,
            @Valid @RequestBody TaskAiSubtasksRequest request) {
        TaskAiBackend backend = TaskAiBackend.fromHeader(aiBackendHeader);
        List<AiProposedTaskDto> body = taskAiService.proposeSubtasks(
                request.getTaskId(),
                request.getFreelancerId(),
                backend);
        return ResponseEntity.ok(body);
    }
}
