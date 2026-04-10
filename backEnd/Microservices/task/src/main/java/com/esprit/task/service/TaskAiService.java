package com.esprit.task.service;

import com.esprit.task.client.AImodelClient;
import com.esprit.task.client.PlanningClient;
import com.esprit.task.client.ProjectClient;
import com.esprit.task.client.TaskAiBackend;
import com.esprit.task.dto.ProjectDto;
import com.esprit.task.dto.TaskStatsExtendedDto;
import com.esprit.task.dto.ai.AiContextRequest;
import com.esprit.task.dto.ai.AiGenerateResponse;
import com.esprit.task.dto.ai.AiProposedTaskDto;
import com.esprit.task.dto.ai.AiPromptRequest;
import com.esprit.task.dto.ai.TaskAiAskTasksResponse;
import com.esprit.task.dto.ai.TaskAiClientBriefResponse;
import com.esprit.task.dto.ai.TaskAiDefinitionOfDoneCriterionDto;
import com.esprit.task.dto.ai.TaskAiDefinitionOfDoneResponse;
import com.esprit.task.dto.ai.TaskAiSuggestDescriptionResponse;
import com.esprit.task.dto.ai.TaskAiWorkloadCoachResponse;
import com.esprit.task.dto.planning.PlanningProgressReportDto;
import com.esprit.task.dto.planning.PlanningProgressUpdateDto;
import com.esprit.task.dto.planning.PlanningProjectProgressStatsDto;
import com.esprit.task.entity.Task;
import com.esprit.task.entity.TaskPriority;
import com.esprit.task.entity.TaskStatus;
import com.esprit.task.exception.EntityNotFoundException;
import com.esprit.task.repository.TaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskAiService {

    /** Avoid huge project descriptions blowing past model/context limits when calling the provider. */
    private static final int MAX_CONTEXT_TEXT_CHARS = 80_000;

    /** Smaller snapshots = faster prompts and less Ollama prefill time. */
    private static final int MAX_COACH_OVERDUE_LINES = 12;
    private static final int MAX_COACH_DUESOON_LINES = 12;
    private static final int MAX_ASK_TASK_ROWS = 36;

    /** Ollama {@code num_predict} caps — keeps coach / Q&amp;A generations short and responsive. */
    private static final int COACH_MAX_OUTPUT_TOKENS = 480;
    private static final int ASK_MAX_OUTPUT_TOKENS = 380;
    private static final int MAX_BRIEF_PROGRESS_UPDATES = 12;
    private static final int MAX_DESC_CHARS_FOR_ASK = 220;
    private static final Pattern CITED_TASK_IDS = Pattern.compile("CITED_TASK_IDS:\\s*([\\d,\\s]+)", Pattern.CASE_INSENSITIVE);
    /** Matches ```json ... ``` (or bare ```) blocks so we can drop model-echoed snapshot payloads. */
    private static final Pattern MARKDOWN_FENCE_BLOCK =
            Pattern.compile("```[a-zA-Z0-9]*\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final AImodelClient aiModelClient;
    private final ProjectClient projectClient;
    private final PlanningClient planningClient;
    private final TaskRepository taskRepository;
    private final TaskFreelancerProjectAccessService accessService;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public TaskAiSuggestDescriptionResponse suggestDescription(
            Long projectId, Long freelancerId, String title, TaskAiBackend backend) {
        if (!accessService.canFreelancerUseProject(freelancerId, projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot use AI for this project");
        }
        ProjectDto project = safeLoadProject(projectId);
        String prompt = buildSuggestDescriptionPrompt(title.trim(), project);
        String text = callGenerate(backend, prompt);
        return TaskAiSuggestDescriptionResponse.builder().description(text).build();
    }

    public List<AiProposedTaskDto> proposeProjectTasks(Long projectId, Long freelancerId, TaskAiBackend backend) {
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
        AiGenerateResponse resp = generateTasks(new AiContextRequest(context));
        String raw = extractAiPayload(resp);
        return parseProposedTasksArray(raw, "tasks");
    }

    public List<AiProposedTaskDto> proposeSubtasks(Long taskId, Long freelancerId, TaskAiBackend backend) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the assignee of this task");
        }
        if (!accessService.canFreelancerUseProject(freelancerId, task.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot use AI for this project");
        }
        String context = buildSubtaskContext(task);
        AiGenerateResponse resp = generateSubtasks(new AiContextRequest(context));
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
        } catch (FeignException e) {
            // Preserve status/body so GlobalExceptionHandler can surface Project-MS or gateway errors.
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not load project details (non-Feign error). Check Task logs and Project microservice.");
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
            sb.append("Project description (context):\n")
                    .append(truncateForAiContext(project.getDescription()))
                    .append("\n");
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
            sb.append("Project description:\n")
                    .append(truncateForAiContext(project.getDescription()))
                    .append("\n\n");
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
            sb.append("Parent task description:\n")
                    .append(truncateForAiContext(task.getDescription()))
                    .append("\n");
        }
        sb.append("Project ID context: ").append(task.getProjectId()).append("\n");
        return sb.toString();
    }

    private String callGenerate(TaskAiBackend backend, String prompt) {
        return callGenerate(backend, prompt, null);
    }

    private String callGenerate(TaskAiBackend backend, String prompt, Integer maxOutputTokens) {
        AiPromptRequest req = new AiPromptRequest(prompt, maxOutputTokens);
        AiGenerateResponse resp = generate(req);
        return extractAiPayload(resp);
    }

    private AiGenerateResponse generate(AiPromptRequest req) {
        return aiModelClient.generate(req);
    }

    private AiGenerateResponse generateTasks(AiContextRequest ctx) {
        return aiModelClient.generateTasks(ctx);
    }

    private AiGenerateResponse generateSubtasks(AiContextRequest ctx) {
        return aiModelClient.generateSubtasks(ctx);
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

    private static String truncateForAiContext(String text) {
        if (text == null || text.length() <= MAX_CONTEXT_TEXT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_CONTEXT_TEXT_CHARS) + "\n\n[... truncated for AI context length ...]";
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

    // --- Platform AI features (workload coach, DoD, ask-my-tasks, client brief) ---

    /**
     * Builds a workload snapshot for the freelancer and asks the LLM for a concise coaching narrative.
     *
     * @param freelancerId assignee whose tasks and stats are included (no other users)
     * @param horizonDays    due-soon window in days (clamped; default applied if null/non-positive)
     * @param backend        AI backend routing for {@code /api/ai/generate}
     * @return prose summary plus bullet highlights parsed from model output
     * @throws ResponseStatusException 400 if {@code freelancerId} null; 5xx on snapshot/AI failures
     */
    public TaskAiWorkloadCoachResponse workloadCoach(Long freelancerId, Integer horizonDays, TaskAiBackend backend) {
        if (freelancerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerId is required");
        }
        int horizon = horizonDays != null && horizonDays > 0 ? Math.min(horizonDays, 365) : 7;
        TaskStatsExtendedDto ext = taskService.getExtendedStatsByFreelancer(
                freelancerId, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        List<Task> overdue = taskService.getOverdueTasks(Optional.empty(), Optional.of(freelancerId));
        List<Task> dueSoon = taskService.findDueSoon(Optional.empty(), Optional.of(freelancerId), horizon);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("extendedStats", ext);
        snap.put("overdueSample", summarizeTasks(overdue, MAX_COACH_OVERDUE_LINES));
        snap.put("dueSoonSample", summarizeTasks(dueSoon, MAX_COACH_DUESOON_LINES));
        snap.put("horizonDays", horizon);
        String json;
        try {
            json = objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build workload snapshot");
        }
        String prompt = "You are a workload coach. A private JSON workload snapshot follows — read it; never repeat or quote it.\n"
                + "Reply in plain English sentences only. Do NOT output JSON, YAML, tables of tasks from the snapshot, "
                + "or markdown code fences (no ```).\n"
                + "Format:\n"
                + "1) One short paragraph (max ~80 words): risks and weekly focus.\n"
                + "2) Then exactly 4-5 lines starting with '- ' — concrete next actions.\n"
                + "No markdown headings, no preamble. Do not invent tasks or dates not in the snapshot.\n\n"
                + json;
        String text = callGenerate(backend, prompt, COACH_MAX_OUTPUT_TOKENS);
        String narrative = sanitizeCoachNarrative(text, ext, overdue, dueSoon, horizon);
        return TaskAiWorkloadCoachResponse.builder()
                .summaryMarkdown(narrative.trim())
                .highlights(extractBulletHighlights(narrative))
                .build();
    }

    /**
     * Small instruction-tuned models sometimes echo the workload snapshot JSON. Strip fenced echoes, detect bare JSON,
     * and fall back to a deterministic summary so users never see raw payloads.
     */
    private String sanitizeCoachNarrative(
            String raw,
            TaskStatsExtendedDto ext,
            List<Task> overdue,
            List<Task> dueSoon,
            int horizon) {
        if (raw == null || raw.isBlank()) {
            log.warn("Workload coach returned empty text; using fallback narrative");
            return buildCoachFallbackNarrative(ext, overdue, dueSoon, horizon);
        }
        String t = stripFenceBlocksIfEcho(raw, this::isEchoedCoachSnapshotJson).trim();
        if (t.isBlank() || isEchoedCoachSnapshotJson(t)) {
            log.warn("Workload coach echoed snapshot JSON; using fallback narrative");
            return buildCoachFallbackNarrative(ext, overdue, dueSoon, horizon);
        }
        return t;
    }

    private String sanitizeAskNarrative(String raw, List<Map<String, Object>> openRows) {
        if (raw == null || raw.isBlank()) {
            return buildAskFallbackAnswer(openRows);
        }
        String t = stripFenceBlocksIfEcho(raw, this::isEchoedAskSnapshotJson).trim();
        if (t.isBlank() || isEchoedAskSnapshotJson(t)) {
            log.warn("Ask-my-tasks echoed snapshot JSON; using deterministic answer");
            return buildAskFallbackAnswer(openRows);
        }
        return t;
    }

    private String stripFenceBlocksIfEcho(String text, Predicate<String> isEcho) {
        Matcher m = MARKDOWN_FENCE_BLOCK.matcher(text);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            String inner = m.group(1).trim();
            if (isEcho.test(inner)) {
                sb.append(text, last, m.start());
                last = m.end();
            }
        }
        sb.append(text.substring(last));
        return sb.toString().trim();
    }

    private boolean isEchoedAskSnapshotJson(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            String norm = normalizeModelJsonPayload(text.trim());
            if (!norm.startsWith("{")) {
                return false;
            }
            JsonNode r = objectMapper.readTree(norm);
            return r.isObject() && r.has("openRootTasks") && r.get("openRootTasks").isArray();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isEchoedCoachSnapshotJson(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            String norm = normalizeModelJsonPayload(text.trim());
            if (!norm.startsWith("{")) {
                return false;
            }
            JsonNode r = objectMapper.readTree(norm);
            if (!r.isObject() || !r.has("extendedStats")) {
                return false;
            }
            return r.has("overdueSample") && r.has("dueSoonSample");
        } catch (Exception e) {
            return false;
        }
    }

    private static String buildAskFallbackAnswer(List<Map<String, Object>> openRows) {
        if (openRows == null || openRows.isEmpty()) {
            return "You have no open root tasks in this list right now. "
                    + "Anything finished or cancelled is excluded; subtasks are not expanded here.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Here is your open work at a glance (tasks referenced by title from your assignments):\n");
        for (Map<String, Object> row : openRows) {
            Object title = row.get("title");
            Object id = row.get("id");
            Object due = row.get("dueDate");
            sb.append("• ");
            sb.append(title != null && !title.toString().isBlank() ? title : "Task #" + id);
            if (due != null && !due.toString().isBlank() && !"null".equalsIgnoreCase(due.toString())) {
                sb.append(" — due ").append(due);
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private static String buildCoachFallbackNarrative(
            TaskStatsExtendedDto ext, List<Task> overdue, List<Task> dueSoon, int horizon) {
        if (ext == null) {
            return "We could not summarize this workload right now. Close the coach and tap Refresh, "
                    + "or try again in a moment.";
        }
        StringBuilder para = new StringBuilder();
        para.append("You have ").append(ext.getTotalTasks()).append(" assigned tasks and subtasks");
        para.append(", including ").append(ext.getOverdueCount()).append(" that are overdue. ");
        para.append("Over the next ").append(horizon).append(" days, prioritize clearing overdue work and guarding ");
        para.append("anything with a due date in that window.");
        List<String> bullets = new ArrayList<>();
        if (overdue != null) {
            for (Task t : overdue) {
                if (bullets.size() >= 3) {
                    break;
                }
                String title = t.getTitle() != null ? t.getTitle() : "Task #" + t.getId();
                bullets.add("Finish or reschedule overdue: " + title);
            }
        }
        if (dueSoon != null) {
            for (Task t : dueSoon) {
                if (bullets.size() >= 5) {
                    break;
                }
                String title = t.getTitle() != null ? t.getTitle() : "Task #" + t.getId();
                String line = "Protect the due date for: " + title;
                if (t.getDueDate() != null) {
                    line += " (" + t.getDueDate() + ")";
                }
                bullets.add(line);
            }
        }
        while (bullets.size() < 4) {
            bullets.add("Confirm due dates on items that still lack them so your week stays predictable.");
            if (bullets.size() >= 4) {
                break;
            }
            bullets.add("Close one IN_PROGRESS item before pulling in new scope.");
            if (bullets.size() >= 4) {
                break;
            }
        }
        StringBuilder out = new StringBuilder();
        out.append(para).append('\n');
        for (String b : bullets) {
            out.append("- ").append(b).append('\n');
        }
        return out.toString().trim();
    }

    private static List<String> extractBulletHighlights(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (t.startsWith("- ") && t.length() > 2) {
                out.add(t.substring(2).trim());
            }
        }
        return out;
    }

    private static List<Map<String, Object>> summarizeTasks(List<Task> tasks, int max) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        int n = Math.min(max, tasks.size());
        for (int i = 0; i < n; i++) {
            Task t = tasks.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
            m.put("priority", t.getPriority() != null ? t.getPriority().name() : null);
            m.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
            m.put("projectId", t.getProjectId());
            if (Boolean.TRUE.equals(t.getSubtask())) {
                m.put("subtask", true);
                m.put("parentTaskId", t.getParentTaskId());
            }
            rows.add(m);
        }
        return rows;
    }

    /**
     * Generates acceptance criteria as structured JSON from the LLM, parsed into DTOs.
     *
     * @param taskId         root task id
     * @param freelancerId   must match task assignee; project access is enforced
     * @param backend        AI backend for generation
     * @return criteria and assumptions; empty lists if model returned minimal content
     * @throws ResponseStatusException 403 if not assignee / no project access; 404 if task missing; 502 if JSON parse fails
     */
    public TaskAiDefinitionOfDoneResponse definitionOfDone(Long taskId, Long freelancerId, TaskAiBackend backend) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(freelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the assignee of this task");
        }
        if (!accessService.canFreelancerUseProject(freelancerId, task.getProjectId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot use AI for this project");
        }
        ProjectDto project = safeLoadProject(task.getProjectId());
        String prompt = buildDefinitionOfDonePrompt(task, project);
        String raw = callGenerate(backend, prompt);
        return parseDefinitionOfDoneJson(raw);
    }

    private static String buildDefinitionOfDonePrompt(Task task, ProjectDto project) {
        StringBuilder sb = new StringBuilder();
        sb.append("Return ONLY valid JSON with no markdown fences or commentary.\n");
        sb.append("Shape: {\"criteria\":[{\"text\":\"string\",\"mustHave\":true}],\"assumptions\":[\"string\"]}\n");
        sb.append("criteria: 4-10 testable acceptance criteria for this task; assumptions: optional clarifications.\n");
        sb.append("Task title: ").append(task.getTitle() != null ? task.getTitle() : "").append('\n');
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            String td = task.getDescription();
            String tdExcerpt = td.length() > 12_000 ? td.substring(0, 12_000) : td;
            sb.append("Task description:\n").append(tdExcerpt).append('\n');
        }
        if (project.getTitle() != null) {
            sb.append("Project: ").append(project.getTitle()).append('\n');
        }
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            sb.append("Project context:\n")
                    .append(truncateForAiContext(project.getDescription()))
                    .append('\n');
        }
        return sb.toString();
    }

    private TaskAiDefinitionOfDoneResponse parseDefinitionOfDoneJson(String raw) {
        try {
            String normalized = normalizeModelJsonPayload(raw);
            JsonNode root = objectMapper.readTree(normalized);
            JsonNode critArr = root.get("criteria");
            List<TaskAiDefinitionOfDoneCriterionDto> criteria = new ArrayList<>();
            if (critArr != null && critArr.isArray()) {
                for (JsonNode n : critArr) {
                    String tx = text(n, "text");
                    if (tx == null || tx.isBlank()) {
                        continue;
                    }
                    boolean must = n.has("mustHave") && n.get("mustHave").asBoolean(false);
                    criteria.add(TaskAiDefinitionOfDoneCriterionDto.builder().text(tx.trim()).mustHave(must).build());
                }
            }
            List<String> assumptions = new ArrayList<>();
            JsonNode as = root.get("assumptions");
            if (as != null && as.isArray()) {
                for (JsonNode n : as) {
                    if (n.isTextual()) {
                        assumptions.add(n.asText());
                    }
                }
            }
            if (criteria.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned no criteria");
            }
            return TaskAiDefinitionOfDoneResponse.builder()
                    .criteria(criteria)
                    .assumptions(assumptions)
                    .build();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not parse definition-of-done JSON");
        }
    }

    /**
     * RAG-lite: answers a natural-language question using a capped JSON list of open work for the assignee.
     *
     * @param freelancerId scope of task snapshot (open root tasks only, capped)
     * @param question     user question; must not be blank
     * @param backend      AI backend for generation
     * @return answer text and optional cited root task ids parsed from a {@code CITED_TASK_IDS:} footer line
     * @throws ResponseStatusException 400 if ids/question invalid
     */
    public TaskAiAskTasksResponse askMyTasks(Long freelancerId, String question, TaskAiBackend backend) {
        if (freelancerId == null || question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerId and question are required");
        }
        List<Task> assigned = taskRepository.findByAssigneeIdOrderByProjectIdAscOrderIndexAsc(freelancerId);
        List<Map<String, Object>> openRows = new ArrayList<>();
        for (Task t : assigned) {
            if (t.getStatus() == TaskStatus.DONE || t.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }
            if (openRows.size() >= MAX_ASK_TASK_ROWS) {
                break;
            }
            openRows.add(compactTaskForAsk(t));
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of("openRootTasks", openRows));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build task snapshot");
        }
        String prompt = "You answer questions about the freelancer's tasks. A private JSON list follows (openRootTasks). "
                + "Read it; never output JSON, YAML, or markdown code fences (no ```), and do not paste or repeat that list.\n"
                + "Reply in short plain sentences (max ~12 lines). Use real task titles from the data. "
                + "If the answer is not in the data, say you cannot tell from this task list.\n"
                + "End with a single last line exactly:\n"
                + "CITED_TASK_IDS: <comma-separated ids or none>\n\n"
                + "Question: " + question.trim() + "\n\n"
                + json;
        String answerFull = callGenerate(backend, prompt, ASK_MAX_OUTPUT_TOKENS).trim();
        List<Long> cited = parseCitedTaskIds(answerFull);
        String answerBody = stripCitedTaskIdsLine(answerFull);
        answerBody = sanitizeAskNarrative(answerBody, openRows);
        return TaskAiAskTasksResponse.builder()
                .answerMarkdown(answerBody)
                .citedTaskIds(cited.isEmpty() ? null : cited)
                .build();
    }

    private Map<String, Object> compactTaskForAsk(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
        m.put("priority", t.getPriority() != null ? t.getPriority().name() : null);
        m.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
        m.put("projectId", t.getProjectId());
        String d = t.getDescription();
        if (d != null && !d.isBlank()) {
            m.put("descriptionPreview", d.length() > MAX_DESC_CHARS_FOR_ASK ? d.substring(0, MAX_DESC_CHARS_FOR_ASK) + "…" : d);
        }
        return m;
    }

    private static List<Long> parseCitedTaskIds(String text) {
        Matcher m = CITED_TASK_IDS.matcher(text);
        if (!m.find()) {
            return List.of();
        }
        String raw = m.group(1).trim();
        if (raw.equalsIgnoreCase("none") || raw.isEmpty()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String p : raw.split(",")) {
            String s = p.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return out;
    }

    private static String stripCitedTaskIdsLine(String text) {
        int idx = text.toUpperCase().lastIndexOf("CITED_TASK_IDS:");
        if (idx < 0) {
            return text;
        }
        return text.substring(0, idx).trim();
    }

    /**
     * Stakeholder brief for project clients: merges Task MS extended stats with Planning progress data when available.
     *
     * @param projectId    project scope
     * @param clientUserId must equal {@link ProjectDto#getClientId()} for the project
     * @param reportFrom   inclusive optional bound for Planning period report
     * @param reportTo     inclusive optional bound for Planning period report
     * @return markdown brief; {@link TaskAiClientBriefResponse#getPlanningDataWarning()} set when Planning Feign fails (degraded task-only context)
     * @throws ResponseStatusException 403 if caller is not the project client
     */
    public TaskAiClientBriefResponse clientStatusBrief(Long projectId, Long clientUserId, LocalDate reportFrom, LocalDate reportTo) {
        ProjectDto project = safeLoadProject(projectId);
        if (project.getClientId() == null || !project.getClientId().equals(clientUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the client for this project");
        }
        TaskStatsExtendedDto taskStats = taskService.getExtendedStatsByProject(projectId);
        String planningWarning = null;
        List<Map<String, Object>> progressRows = Collections.emptyList();
        PlanningProjectProgressStatsDto planningStats = null;
        PlanningProgressReportDto reportDto = null;
        try {
            List<PlanningProgressUpdateDto> all = planningClient.listProgressUpdatesByProject(projectId);
            progressRows = trimProgressUpdatesForBrief(all);
            planningStats = planningClient.getProjectProgressStats(projectId);
            reportDto = planningClient.getProgressReport(projectId, reportFrom, reportTo);
        } catch (FeignException e) {
            planningWarning = "Progress-update context from Planning could not be loaded; brief uses task data only.";
            log.warn("Planning Feign failed for client brief project {}: {}", projectId, e.getMessage());
        } catch (Exception e) {
            planningWarning = "Progress-update context from Planning could not be loaded; brief uses task data only.";
            log.warn("Planning error for client brief project {}: {}", projectId, e.getMessage());
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectTitle", project.getTitle());
        payload.put("taskBoardStats", taskStats);
        payload.put("recentProgressUpdates", progressRows);
        payload.put("planningAggregateStats", planningStats);
        payload.put("planningPeriodReport", reportDto);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build brief snapshot");
        }
        String prompt = "Write a professional, neutral status brief for a non-technical client stakeholder.\n"
                + "Use ONLY facts present in the JSON (task board stats, optional formal progress updates from the team).\n"
                + "Structure: short executive paragraph, then bullet milestones. Do not blame individuals.\n"
                + "If progress percentages appear, cite them exactly as given. Plain text.\n\n"
                + json;
        String brief = callGenerate(TaskAiBackend.OLLAMA, prompt).trim();
        return TaskAiClientBriefResponse.builder()
                .briefMarkdown(brief)
                .planningDataWarning(planningWarning)
                .build();
    }

    private static List<Map<String, Object>> trimProgressUpdatesForBrief(List<PlanningProgressUpdateDto> all) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        List<PlanningProgressUpdateDto> sorted = new ArrayList<>(all);
        sorted.sort((a, b) -> {
            if (a.getCreatedAt() == null) {
                return 1;
            }
            if (b.getCreatedAt() == null) {
                return -1;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        List<Map<String, Object>> out = new ArrayList<>();
        int n = Math.min(MAX_BRIEF_PROGRESS_UPDATES, sorted.size());
        for (final PlanningProgressUpdateDto u : sorted.subList(0, n)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", u.getTitle());
            m.put("progressPercentage", u.getProgressPercentage());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            String desc = u.getDescription();
            if (desc != null && desc.length() > 1_500) {
                desc = desc.substring(0, 1_500) + "…";
            }
            m.put("description", desc);
            out.add(m);
        }
        return out;
    }
}
