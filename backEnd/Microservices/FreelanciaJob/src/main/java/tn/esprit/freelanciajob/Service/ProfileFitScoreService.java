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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tn.esprit.freelanciajob.Client.ExperienceClient;
import tn.esprit.freelanciajob.Client.SkillClient;
import tn.esprit.freelanciajob.Dto.ExperienceDto;
import tn.esprit.freelanciajob.Dto.Skills;
import tn.esprit.freelanciajob.Dto.response.FitScoreResponse;
import tn.esprit.freelanciajob.Entity.Job;
import tn.esprit.freelanciajob.Repository.JobRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileFitScoreService {

    private static final Set<String> VALID_TIERS = Set.of(
            "STRONG_MATCH", "GOOD_MATCH", "PARTIAL_MATCH", "LOW_MATCH"
    );

    private final JobRepository jobRepository;
    private final SkillClient skillClient;
    private final ExperienceClient experienceClient;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

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
                System.err.println("[ProfileFitScoreService] Could not load API_KEY from .env: " + e.getMessage());
            }
        }
    }

    public FitScoreResponse computeFitScore(Long jobId, Long freelancerId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + jobId));

        List<Skills> skills = fetchSkillsSafely(freelancerId);
        List<ExperienceDto> experiences = fetchExperiencesSafely(freelancerId);

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(job, skills, experiences);

        String rawResponse = callExternalApi(systemPrompt, userPrompt);
        return parseAndValidate(rawResponse);
    }

    private List<Skills> fetchSkillsSafely(Long freelancerId) {
        try {
            List<Skills> result = skillClient.getSkillsByUserId(freelancerId);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("[ProfileFitScoreService] Could not fetch skills for freelancer " + freelancerId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ExperienceDto> fetchExperiencesSafely(Long freelancerId) {
        try {
            List<ExperienceDto> result = experienceClient.getExperiencesByUserId(freelancerId);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("[ProfileFitScoreService] Could not fetch experiences for freelancer " + freelancerId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildSystemPrompt() {
        return """
                You are an expert freelance recruiter evaluating how well a freelancer's profile matches a job posting.
                Return ONLY a raw JSON object — no markdown, no code blocks, no extra text — with EXACTLY this schema:
                {
                  "score": <integer 0-100>,
                  "tier": "<STRONG_MATCH|GOOD_MATCH|PARTIAL_MATCH|LOW_MATCH>",
                  "summary": "<1-2 sentence overview of the match>",
                  "matchedSkills": ["<skill name>", ...],
                  "missingSkills": ["<skill name>", ...],
                  "recommendations": ["<actionable tip>", ...]
                }
                Tier rules (strictly enforce):
                  STRONG_MATCH  = score 80-100
                  GOOD_MATCH    = score 60-79
                  PARTIAL_MATCH = score 40-59
                  LOW_MATCH     = score 0-39
                matchedSkills: skills the freelancer has that appear in the job required skills.
                missingSkills: skills the job requires that the freelancer does not have.
                recommendations: 1-3 short, actionable tips to improve the freelancer's fit.
                Do not include any explanation outside the JSON object.
                """;
    }

    private String buildUserPrompt(Job job, List<Skills> skills, List<ExperienceDto> experiences) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== JOB POSTING ===\n");
        sb.append("Title: ").append(job.getTitle()).append("\n");
        sb.append("Category: ").append(job.getCategory()).append("\n");
        sb.append("Description: ").append(job.getDescription()).append("\n");

        if (job.getRequiredSkillIds() != null && !job.getRequiredSkillIds().isEmpty()) {
            sb.append("Required skill IDs: ").append(job.getRequiredSkillIds()).append("\n");
        }

        sb.append("\n=== FREELANCER PROFILE ===\n");

        if (skills.isEmpty()) {
            sb.append("Skills: (none listed)\n");
        } else {
            sb.append("Skills:\n");
            for (Skills s : skills) {
                sb.append("  - ").append(s.getName());
                if (s.getDomain() != null && !s.getDomain().isBlank()) {
                    sb.append(" [").append(s.getDomain()).append("]");
                }
                if (s.getDescription() != null && !s.getDescription().isBlank()) {
                    sb.append(": ").append(s.getDescription());
                }
                sb.append("\n");
            }
        }

        if (experiences.isEmpty()) {
            sb.append("Experiences: (none listed)\n");
        } else {
            sb.append("Experiences:\n");
            for (ExperienceDto exp : experiences) {
                sb.append("  - ").append(exp.getTitle());
                if (exp.getCompanyOrClientName() != null && !exp.getCompanyOrClientName().isBlank()) {
                    sb.append(" at ").append(exp.getCompanyOrClientName());
                }
                if (exp.getDomain() != null && !exp.getDomain().isBlank()) {
                    sb.append(" (").append(exp.getDomain()).append(")");
                }
                if (exp.getStartDate() != null && exp.getEndDate() != null) {
                    sb.append(" [").append(exp.getStartDate()).append(" – ").append(exp.getEndDate()).append("]");
                }
                if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                    sb.append("\n    Description: ").append(exp.getDescription());
                }
                if (exp.getKeyTasks() != null && !exp.getKeyTasks().isEmpty()) {
                    sb.append("\n    Key tasks: ").append(String.join(", ", exp.getKeyTasks()));
                }
                sb.append("\n");
            }
        }

        sb.append("\nEvaluate the match and return the JSON score object.");
        return sb.toString();
    }

    private String callExternalApi(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", apiModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 512,
                "stream", false,
                "temperature", 0.3
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from AI service");
            }
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("AI service unavailable: " + e.getMessage(), e);
        }
    }

    private FitScoreResponse parseAndValidate(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Strip markdown fences if the model added them despite instructions
            content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            JsonNode node = objectMapper.readTree(content);

            int score = Math.max(0, Math.min(100, node.path("score").asInt(0)));

            String tier = node.path("tier").asText("");
            if (!VALID_TIERS.contains(tier)) {
                tier = deriveeTierFromScore(score);
            }

            String summary = node.path("summary").asText("");

            List<String> matchedSkills = readStringList(node.path("matchedSkills"));
            List<String> missingSkills = readStringList(node.path("missingSkills"));
            List<String> recommendations = readStringList(node.path("recommendations"));

            return FitScoreResponse.builder()
                    .score(score)
                    .tier(tier)
                    .summary(summary)
                    .matchedSkills(matchedSkills)
                    .missingSkills(missingSkills)
                    .recommendations(recommendations)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse fit score response from AI: " + e.getMessage(), e);
        }
    }

    private String deriveeTierFromScore(int score) {
        if (score >= 80) return "STRONG_MATCH";
        if (score >= 60) return "GOOD_MATCH";
        if (score >= 40) return "PARTIAL_MATCH";
        return "LOW_MATCH";
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isArray()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        node.forEach(item -> {
            String text = item.asText("").trim();
            if (!text.isEmpty()) result.add(text);
        });
        return result;
    }
}
