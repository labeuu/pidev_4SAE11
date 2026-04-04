package com.esprit.task.service;

import com.esprit.task.client.AImodelClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.ai.AiContextRequest;
import com.esprit.task.dto.ai.AiGenerateResponse;
import com.esprit.task.dto.ai.AiProposedTaskDto;
import com.esprit.task.dto.ai.AiPromptRequest;
import com.esprit.task.dto.ai.TaskAiSuggestDescriptionResponse;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskAiService {

    private final AImodelClient aiModelClient;
    private final ProjectClient projectClient;
    private final TaskRepository taskRepository;
    private final TaskFreelancerProjectAccessService accessService;
    private final ObjectMapper objectMapper;

    public TaskAiSuggestDescriptionResponse suggestDescription(Long projectId, Long freelancerId, String title) {
        if (!accessService.canFreelancerUseProject(freelancerId, projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot use AI for this project");
        }
        ProjectDto project = safeLoadProject(projectId);
        String prompt = buildSuggestDescriptionPrompt(title.trim(), project);
        String text = callGenerate(prompt);
        return TaskAiSuggestDescriptionResponse.builder().description(text).build();
    }

    public List<AiProposedTaskDto> proposeProjectTasks(Long projectId, Long freelancerId) {
        if (!accessService.canFreelancerUseProject(freelancerId, projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot use AI for this project");
        }
        ProjectDto project = safeLoadProject(projectId);
        List<String> existingTitles = taskRepository
                .findByProjectIdOrderByOrderIndexAsc(projectId)
                .stream()
                .map(Task::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
        String context = buildProjectTasksContext(project, existingTitles);
        AiGenerateResponse resp = aiModelClient.generateTasks(new AiContextRequest(context));
        String raw = extractAiPayload(resp);
        return parseProposedTasksArray(raw, "tasks");
    }

    public List<AiProposedTaskDto> proposeSubtasks(Long taskId, Long freelancerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the assignee of this task");
        }
        if (!accessService.canFreelancerUseProject(freelancerId, task.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot use AI for this project");
        }
        String context = buildSubtaskContext(task);
        AiGenerateResponse resp = aiModelClient.generateSubtasks(new AiContextRequest(context));
        String raw = extractAiPayload(resp);
        return parseProposedTasksArray(raw, "subtasks");
    }

    private ProjectDto safeLoadProject(Long projectId) {
        try {
            ProjectDto p = projectClient.getProjectById(projectId);
            if (p == null || p.getId() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
            }
            return p;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not load project details");
        }
    }

    private static String buildSuggestDescriptionPrompt(String title, ProjectDto project) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are helping a freelancer write a task description.\n");
        sb.append("Task title: ").append(title).append("\n");
        if (project.getTitle() != null && !project.getTitle().isBlank()) {
            sb.append("Project: ").append(project.getTitle()).append("\n");
        }
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            sb.append("Project description (context):\n").append(project.getDescription()).append("\n");
        }
        sb.append("\nWrite a concise, actionable task description (2–6 sentences). ");
        sb.append("Return plain text only, no markdown, no title line.");
        return sb.toString();
    }

    private static String buildProjectTasksContext(ProjectDto project, List<String> existingTitles) {
        StringBuilder sb = new StringBuilder();
        if (project.getTitle() != null) {
            sb.append("Project title: ").append(project.getTitle()).append("\n\n");
        }
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            sb.append("Project description:\n").append(project.getDescription()).append("\n\n");
        }
        if (!existingTitles.isEmpty()) {
            sb.append("Existing top-level task titles (avoid duplicating these):\n");
            for (String t : existingTitles) {
                sb.append("- ").append(t).append("\n");
            }
            sb.append("\n");
        }
        sb.append("Propose practical work items for this project for a freelancer.");
        return sb.toString();
    }

    private static String buildSubtaskContext(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append("Parent task title: ").append(task.getTitle() != null ? task.getTitle() : "").append("\n");
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            sb.append("Parent task description:\n").append(task.getDescription()).append("\n");
        }
        sb.append("Project ID context: ").append(task.getProjectId()).append("\n");
        return sb.toString();
    }

    private String callGenerate(String prompt) {
        AiGenerateResponse resp = aiModelClient.generate(new AiPromptRequest(prompt));
        return extractAiPayload(resp);
    }

    private static String extractAiPayload(AiGenerateResponse resp) {
        if (resp == null || !resp.isSuccess() || resp.getData() == null || resp.getData().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned an empty response");
        }
        return resp.getData().trim();
    }

    private List<AiProposedTaskDto> parseProposedTasksArray(String json, String arrayKey) {
        try {
            String normalized = normalizeModelJsonPayload(json);
            JsonNode root = objectMapper.readTree(normalized);
            JsonNode arr = root.get(arrayKey);
            if (arr == null || !arr.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response missing '" + arrayKey + "' array");
            }
            List<AiProposedTaskDto> out = new ArrayList<>();
            for (JsonNode node : arr) {
                String t = text(node, "title");
                String d = text(node, "description");
                String p = text(node, "priority");
                if (t == null || t.isBlank()) {
                    continue;
                }
                out.add(AiProposedTaskDto.builder()
                        .title(t.trim())
                        .description(d != null ? d.trim() : "")
                        .suggestedPriority(mapPriority(p))
                        .suggestedDueDate(parseSuggestedDueDate(node))
                        .build());
            }
            if (out.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned no usable items");
            }
            return out;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not parse AI JSON");
        }
    }

    /** Strips thinking blocks, markdown fences, and isolates the first top-level JSON object. */
    static String normalizeModelJsonPayload(String raw) {
        if (raw == null) {
            return "";
        }
        String s = stripMarkdownCodeFence(raw.trim());
        return firstTopLevelJsonObject(s);
    }

    private static String stripMarkdownCodeFence(String s) {
        String t = s.trim();
        if (!t.startsWith("```")) {
            return t;
        }
        int firstNl = t.indexOf('\n');
        if (firstNl > 0) {
            t = t.substring(firstNl + 1);
        } else {
            return t;
        }
        int fence = t.lastIndexOf("```");
        if (fence > 0) {
            t = t.substring(0, fence);
        }
        return t.trim();
    }

    private static String firstTopLevelJsonObject(String s) {
        int start = s.indexOf('{');
        if (start < 0) {
            return s;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return s.substring(start, i + 1);
                }
            }
        }
        return s.substring(start);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static LocalDate parseSuggestedDueDate(JsonNode node) {
        String raw = text(node, "dueDate");
        if (raw == null || raw.isBlank()) {
            raw = text(node, "due_date");
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            if (s.length() >= 10) {
                try {
                    return LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private static TaskPriority mapPriority(String raw) {
        if (raw == null) {
            return TaskPriority.MEDIUM;
        }
        switch (raw.trim().toLowerCase()) {
            case "low":
                return TaskPriority.LOW;
            case "high":
                return TaskPriority.HIGH;
            case "urgent":
                return TaskPriority.URGENT;
            case "medium":
            default:
                return TaskPriority.MEDIUM;
        }
    }
}
