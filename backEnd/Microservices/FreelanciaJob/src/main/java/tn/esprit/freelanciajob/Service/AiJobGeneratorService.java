package tn.esprit.freelanciajob.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.freelanciajob.Dto.response.GeneratedJobDraft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJobGeneratorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model:openai/gpt-5.2}")
    private String apiModel;

    @PostConstruct
    public void init() {
        String resolvedKey = resolveApiKey();
        if (hasText(resolvedKey)) {
            this.apiKey = resolvedKey.trim();
            log.info("AI API key resolved successfully for job generation.");
        } else {
            log.warn("AI API key could not be resolved. Job generation will use fallback drafts.");
        }
    }

    public GeneratedJobDraft generateJobDraft(String userPrompt) {
        refreshApiKeyIfNeeded();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI key missing. Falling back to local draft generation.");
            return buildFallbackDraft(userPrompt);
        }
        String systemPrompt = """
                You are an expert freelance platform assistant.
                The user describes a project idea in one sentence.
                You must return ONLY a raw JSON object (no markdown, no code blocks) with exactly these fields:
                {
                  "title": "string – concise job title (max 80 chars)",
                  "description": "string – detailed job description (200-400 words) covering objectives, deliverables, and requirements",
                  "requiredSkills": ["string", ...] – list of 3-6 relevant technical skill names,
                  "budgetMin": number – minimum budget in USD,
                  "budgetMax": number – maximum budget in USD,
                  "currency": "USD",
                  "estimatedDurationWeeks": number – realistic project duration in weeks,
                  "category": "string – one of: Web Development, Mobile Development, UI/UX Design, Data Science, DevOps, Content Writing, Marketing, Backend",
                  "locationType": "string – one of: REMOTE, ONSITE, HYBRID"
                }
                """;

        Map<String, Object> requestBody = Map.of(
                "model", apiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 1024,
                "stream", false,
                "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return parseDraft(response.getBody());
        } catch (ResponseStatusException e) {
            log.warn("AI draft generation failed with status {}. Falling back. Reason: {}",
                    e.getStatusCode(), e.getReason());
            return buildFallbackDraft(userPrompt);
        } catch (Exception e) {
            log.warn("AI service call failed. Falling back to local draft generation.", e);
            return buildFallbackDraft(userPrompt);
        }
    }

    private GeneratedJobDraft parseDraft(String jsonResponse) {
        try {
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty AI response");
            }

            JsonNode root = objectMapper.readTree(jsonResponse);

            // Never use get(0) — it returns null when choices is empty/missing; use path() instead.
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                JsonNode err = root.path("error");
                String hint = err.isMissingNode()
                        ? jsonResponse.substring(0, Math.min(400, jsonResponse.length()))
                        : err.toString();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI response has no choices[] (check API key / model / quota). Snippet: " + hint);
            }

            String content = choices.path(0).path("message").path("content").asText("").trim();
            if (content.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI message content is empty");
            }

            // Strip markdown fences if present
            content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            JsonNode draft = parseDraftJson(content);

            List<String> skills = new ArrayList<>();
            JsonNode skillsNode = draft.path("requiredSkills");
            if (skillsNode.isArray()) {
                skillsNode.forEach(s -> skills.add(s.asText()));
            }

            return GeneratedJobDraft.builder()
                    .title(draft.path("title").asText(""))
                    .description(draft.path("description").asText(""))
                    .requiredSkills(skills)
                    .budgetMin(draft.path("budgetMin").isMissingNode() ? null
                            : BigDecimal.valueOf(draft.path("budgetMin").asDouble()))
                    .budgetMax(draft.path("budgetMax").isMissingNode() ? null
                            : BigDecimal.valueOf(draft.path("budgetMax").asDouble()))
                    .currency(draft.path("currency").asText("USD"))
                    .estimatedDurationWeeks(draft.path("estimatedDurationWeeks").isMissingNode() ? null
                            : draft.path("estimatedDurationWeeks").asInt())
                    .category(draft.path("category").asText(""))
                    .locationType(draft.path("locationType").asText("REMOTE"))
                    .build();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to parse AI draft response: " + e.getMessage(), e);
        }
    }

    /**
     * Parse the model's text content into a JSON object; tolerate extra prose by extracting the first {...} block.
     */
    private JsonNode parseDraftJson(String content) throws JsonProcessingException {
        try {
            return objectMapper.readTree(content);
        } catch (Exception first) {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readTree(content.substring(start, end + 1));
            }
            throw first;
        }
    }

    /**
     * Resilient fallback used when external AI provider is unavailable.
     * Keeps the endpoint usable instead of returning 5xx.
     */
    private GeneratedJobDraft buildFallbackDraft(String userPrompt) {
        String prompt = userPrompt == null ? "" : userPrompt.trim();
        String normalized = prompt.toLowerCase(Locale.ROOT);

        String category = inferCategory(normalized);
        String locationType = inferLocationType(normalized);
        List<String> skills = inferSkills(normalized, category);

        String titleBase = prompt.isBlank() ? "Project Opportunity" : prompt;
        if (titleBase.length() > 80) {
            titleBase = titleBase.substring(0, 80).trim();
        }

        String description = "We are looking for a freelancer to deliver this project end-to-end. "
                + "Scope includes analysis, implementation, testing, and handover documentation. "
                + "Please share your relevant experience, timeline estimate, and approach. "
                + "Deliverables should include production-ready output, clear progress updates, and final support for adjustments. "
                + (prompt.isBlank() ? "" : "Project context: " + prompt + ".");

        return GeneratedJobDraft.builder()
                .title(titleBase)
                .description(description)
                .requiredSkills(skills)
                .budgetMin(BigDecimal.valueOf(500))
                .budgetMax(BigDecimal.valueOf(2000))
                .currency("USD")
                .estimatedDurationWeeks(4)
                .category(category)
                .locationType(locationType)
                .build();
    }

    private String inferCategory(String normalizedPrompt) {
        if (containsAny(normalizedPrompt, "android", "ios", "mobile", "flutter", "react native")) {
            return "Mobile Development";
        }
        if (containsAny(normalizedPrompt, "ui", "ux", "figma", "design")) {
            return "UI/UX Design";
        }
        if (containsAny(normalizedPrompt, "ml", "ai", "data", "analytics", "model")) {
            return "Data Science";
        }
        if (containsAny(normalizedPrompt, "devops", "docker", "kubernetes", "ci/cd", "pipeline")) {
            return "DevOps";
        }
        if (containsAny(normalizedPrompt, "content", "copywriting", "article", "blog")) {
            return "Content Writing";
        }
        if (containsAny(normalizedPrompt, "marketing", "seo", "ads", "campaign")) {
            return "Marketing";
        }
        if (containsAny(normalizedPrompt, "api", "spring", "node", "backend", "database")) {
            return "Backend";
        }
        return "Web Development";
    }

    private String inferLocationType(String normalizedPrompt) {
        if (containsAny(normalizedPrompt, "onsite", "on-site")) {
            return "ONSITE";
        }
        if (containsAny(normalizedPrompt, "hybrid")) {
            return "HYBRID";
        }
        return "REMOTE";
    }

    private List<String> inferSkills(String normalizedPrompt, String category) {
        List<String> skills = new ArrayList<>();
        if (containsAny(normalizedPrompt, "angular")) skills.add("Angular");
        if (containsAny(normalizedPrompt, "react")) skills.add("React");
        if (containsAny(normalizedPrompt, "spring")) skills.add("Spring Boot");
        if (containsAny(normalizedPrompt, "java")) skills.add("Java");
        if (containsAny(normalizedPrompt, "sql", "mysql", "postgres")) skills.add("SQL");
        if (containsAny(normalizedPrompt, "docker")) skills.add("Docker");
        if (containsAny(normalizedPrompt, "figma")) skills.add("Figma");
        if (containsAny(normalizedPrompt, "python")) skills.add("Python");

        if (skills.isEmpty()) {
            switch (category) {
                case "Mobile Development" -> skills.addAll(List.of("Flutter", "REST APIs", "Firebase"));
                case "UI/UX Design" -> skills.addAll(List.of("Figma", "Wireframing", "Prototyping"));
                case "Data Science" -> skills.addAll(List.of("Python", "Machine Learning", "Data Analysis"));
                case "DevOps" -> skills.addAll(List.of("Docker", "CI/CD", "Kubernetes"));
                case "Backend" -> skills.addAll(List.of("Java", "Spring Boot", "SQL"));
                default -> skills.addAll(List.of("JavaScript", "REST APIs", "Git"));
            }
        }
        return skills;
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) return true;
        }
        return false;
    }

    private String resolveApiKey() {
        if (hasText(apiKey) && !apiKey.startsWith("${")) {
            return apiKey;
        }

        String[] propertyKeys = {
                "ai.api.key",
                "AI_API_KEY",
                "API_KEY",
                "OPENAI_API_KEY"
        };
        for (String propertyKey : propertyKeys) {
            String candidate = environment.getProperty(propertyKey);
            if (hasText(candidate) && !candidate.startsWith("${")) {
                return candidate;
            }
        }

        String dotenvKey = readDotenvKey();
        if (hasText(dotenvKey)) {
            return dotenvKey;
        }

        String localPropertyKey = readKeyFromClasspathProperties("application-local.properties");
        if (hasText(localPropertyKey)) {
            return localPropertyKey;
        }

        return readKeyFromClasspathProperties("application.properties");
    }

    private void refreshApiKeyIfNeeded() {
        if (hasText(apiKey)) {
            return;
        }

        String resolvedKey = resolveApiKey();
        if (hasText(resolvedKey)) {
            this.apiKey = resolvedKey.trim();
            log.info("AI API key resolved on demand: {}", maskKey(this.apiKey));
        }
    }

    private String readDotenvKey() {
        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .ignoreIfMissing()
                    .directory("../../")
                    .filename(".env")
                    .load();

            String[] dotenvKeys = {"AI_API_KEY", "API_KEY", "OPENAI_API_KEY"};
            for (String dotenvKey : dotenvKeys) {
                String candidate = dotenv.get(dotenvKey);
                if (hasText(candidate)) {
                    return candidate;
                }
            }
        } catch (Exception e) {
            log.warn("Could not load AI API key from .env: {}", e.getMessage());
        }
        return null;
    }

    private String readKeyFromClasspathProperties(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(fileName);
            if (!resource.exists()) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(resource.getInputStream());

            String raw = properties.getProperty("ai.api.key");
            if (!hasText(raw)) {
                return null;
            }

            if (!raw.startsWith("${")) {
                return raw;
            }

            return extractPlaceholderDefault(raw);
        } catch (Exception e) {
            log.warn("Could not read {} while resolving AI API key: {}", fileName, e.getMessage());
            return null;
        }
    }

    private String extractPlaceholderDefault(String placeholder) {
        int colonIndex = placeholder.indexOf(':');
        int endIndex = placeholder.lastIndexOf('}');
        if (colonIndex < 0 || endIndex <= colonIndex) {
            return null;
        }
        String defaultValue = placeholder.substring(colonIndex + 1, endIndex).trim();
        return hasText(defaultValue) ? defaultValue : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String maskKey(String value) {
        if (!hasText(value)) {
            return "<missing>";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
