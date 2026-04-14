package tn.esprit.freelanciajob.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.freelanciajob.Dto.response.GeneratedJobDraft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiJobGeneratorService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model:openai/gpt-5.2}")
    private String apiModel;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("${")) {
            try {
                io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                        .ignoreIfMissing()
                        .directory("../../")
                        .filename(".env")
                        .load();
                String envKey = dotenv.get("API_KEY");
                if (envKey != null && !envKey.isBlank()) {
                    this.apiKey = envKey;
                }
            } catch (Exception e) {
                System.err.println("[AiJobGeneratorService] Could not load API_KEY from .env: " + e.getMessage());
            }
        }
    }

    public GeneratedJobDraft generateJobDraft(String userPrompt) {
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
        } catch (Exception e) {
            throw new RuntimeException("AI service unavailable: " + e.getMessage(), e);
        }
    }

    private GeneratedJobDraft parseDraft(String jsonResponse) {
        try {
            if (jsonResponse == null) throw new RuntimeException("Empty AI response");

            JsonNode root = objectMapper.readTree(jsonResponse);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Strip markdown fences if present
            content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            JsonNode draft = objectMapper.readTree(content);

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

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI draft response: " + e.getMessage(), e);
        }
    }
}
