package com.esprit.task.controller;

import com.esprit.task.client.TaskAiBackend;
import com.esprit.task.dto.ai.AiProposedTaskDto;
import com.esprit.task.dto.ai.TaskAiAskTasksRequest;
import com.esprit.task.dto.ai.TaskAiAskTasksResponse;
import com.esprit.task.dto.ai.TaskAiClientBriefRequest;
import com.esprit.task.dto.ai.TaskAiClientBriefResponse;
import com.esprit.task.dto.ai.TaskAiDefinitionOfDoneRequest;
import com.esprit.task.dto.ai.TaskAiDefinitionOfDoneResponse;
import com.esprit.task.dto.ai.TaskAiProjectContextRequest;
import com.esprit.task.dto.ai.TaskAiSubtasksRequest;
import com.esprit.task.dto.ai.TaskAiSuggestDescriptionRequest;
import com.esprit.task.dto.ai.TaskAiSuggestDescriptionResponse;
import com.esprit.task.dto.ai.TaskAiWorkloadCoachRequest;
import com.esprit.task.dto.ai.TaskAiWorkloadCoachResponse;
import com.esprit.task.service.TaskAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @PostMapping("/workload-coach")
    @Operation(summary = "AI workload coach", description = "Narrative coaching from freelancer workload snapshot (stats, overdue, due-soon).")
    @ApiResponse(responseCode = "200", description = "Coach text and bullet highlights")
    public ResponseEntity<TaskAiWorkloadCoachResponse> workloadCoach(
            @RequestHeader(value = "X-AI-Backend", required = false) String aiBackendHeader,
            @Valid @RequestBody TaskAiWorkloadCoachRequest request) {
        TaskAiBackend backend = TaskAiBackend.fromHeader(aiBackendHeader);
        return ResponseEntity.ok(taskAiService.workloadCoach(
                request.getFreelancerId(), request.getHorizonDays(), backend));
    }

    @PostMapping("/definition-of-done")
    @Operation(summary = "AI definition of done", description = "Structured acceptance criteria checklist for a task (assignee only).")
    @ApiResponse(responseCode = "200", description = "Criteria and assumptions")
    public ResponseEntity<TaskAiDefinitionOfDoneResponse> definitionOfDone(
            @RequestHeader(value = "X-AI-Backend", required = false) String aiBackendHeader,
            @Valid @RequestBody TaskAiDefinitionOfDoneRequest request) {
        TaskAiBackend backend = TaskAiBackend.fromHeader(aiBackendHeader);
        return ResponseEntity.ok(taskAiService.definitionOfDone(
                request.getTaskId(), request.getFreelancerId(), backend));
    }

    @PostMapping("/ask-tasks")
    @Operation(summary = "Ask my tasks", description = "Natural-language Q&A over open root tasks assigned to the freelancer.")
    @ApiResponse(responseCode = "200", description = "Answer and optional cited task ids")
    public ResponseEntity<TaskAiAskTasksResponse> askTasks(
            @RequestHeader(value = "X-AI-Backend", required = false) String aiBackendHeader,
            @Valid @RequestBody TaskAiAskTasksRequest request) {
        TaskAiBackend backend = TaskAiBackend.fromHeader(aiBackendHeader);
        return ResponseEntity.ok(taskAiService.askMyTasks(
                request.getFreelancerId(), request.getQuestion(), backend));
    }

    @PostMapping("/client-status-brief")
    @Operation(summary = "AI client status brief", description = "Stakeholder update from task board + Planning progress (project client only).")
    @ApiResponse(responseCode = "200", description = "Brief markdown; planningDataWarning when Planning is unavailable")
    public ResponseEntity<TaskAiClientBriefResponse> clientStatusBrief(
            @RequestHeader(value = "X-AI-Backend", required = false) String aiBackendHeader,
            @Valid @RequestBody TaskAiClientBriefRequest request) {
        TaskAiBackend.fromHeader(aiBackendHeader);
        return ResponseEntity.ok(taskAiService.clientStatusBrief(
                request.getProjectId(),
                request.getClientUserId(),
                request.getReportFrom(),
                request.getReportTo()));
    }
}
