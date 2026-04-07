package com.esprit.task.controller;

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
            @Valid @RequestBody TaskAiSuggestDescriptionRequest request) {
        TaskAiSuggestDescriptionResponse body = taskAiService.suggestDescription(
                request.getProjectId(),
                request.getFreelancerId(),
                request.getTitle());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/propose-project-tasks")
    @Operation(summary = "Propose new top-level tasks for a project")
    public ResponseEntity<List<AiProposedTaskDto>> proposeProjectTasks(
            @Valid @RequestBody TaskAiProjectContextRequest request) {
        List<AiProposedTaskDto> body = taskAiService.proposeProjectTasks(
                request.getProjectId(),
                request.getFreelancerId());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/propose-subtasks")
    @Operation(summary = "Propose subtasks for a root task")
    public ResponseEntity<List<AiProposedTaskDto>> proposeSubtasks(
            @Valid @RequestBody TaskAiSubtasksRequest request) {
        List<AiProposedTaskDto> body = taskAiService.proposeSubtasks(
                request.getTaskId(),
                request.getFreelancerId());
        return ResponseEntity.ok(body);
    }
}
