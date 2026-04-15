package org.example.subcontracting.coach;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.coach.dto.CoachInsightRequest;
import org.example.subcontracting.coach.dto.CoachInsightResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachInsightAiService {

    @Qualifier("anthropicRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.api.model:claude-3-5-haiku-20241022}")
    private String anthropicModel;

    @Value("${anthropic.api.url:https://api.anthropic.com/v1/messages}")
    private String anthropicUrl;

    private static final String SYSTEM_PROMPT = """
            Tu es un coach expert en gestion de sous-traitance freelance.
            Tu analyses les données fournies et retournes UNIQUEMENT un JSON valide sans markdown ni texte autour,
            avec cette structure exacte :
            {
              "globalRisk": { "score": <0-100>, "level": "LOW|MEDIUM|HIGH|CRITICAL", "summary": "<une phrase>" },
              "causes": [
                { "title": "...", "detail": "...", "impact": <1-10> }
              ],
              "actions": [
                { "title": "...", "detail": "...", "priority": "URGENT|THIS_WEEK|OPTIONAL", "expectedRiskReduction": <0-20> }
              ],
              "expectedImpact": {
                "riskReductionIfApplied": <0-30>,
                "newEstimatedScore": <0-100>,
                "confidenceLevel": "HIGH|MEDIUM|LOW"
              },
              "professionalTip": "<une astuce mémorable>",
              "urgencyVerdict": "PROCEED|REVIEW_FIRST|DONT_PROCEED",
              "whatIf": { "scenario": "...", "riskDelta": <int optionnel> }
            }
            Règles : exactement 3 entrées dans "causes" et 3 dans "actions". "whatIf" est obligatoire pour analyse avancée (paid), peut être minimal pour gratuit.
            """;

    public CoachInsightResponse generate(CoachInsightRequest req, boolean advanced) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return heuristicInsight(req, advanced);
        }
        try {
            String userJson = objectMapper.writeValueAsString(Map.of(
                    "subcontractId", req.getSubcontractId(),
                    "scope", req.getScope(),
                    "category", req.getCategory(),
                    "budget", req.getBudget(),
                    "durationDays", req.getDurationDays(),
                    "subcontractorId", req.getSubcontractorId(),
                    "advanced", advanced
            ));
            String prompt = (advanced ? "Analyse avancée (what-if, comparaisons) : " : "Analyse initiale (gratuite) : ")
                    + userJson;
            String raw = callClaude(prompt);
            String json = extractJsonObject(raw);
            CoachInsightResponse parsed = objectMapper.readValue(json, CoachInsightResponse.class);
            normalize(parsed, advanced);
            return parsed;
        } catch (Exception e) {
            log.warn("[COACH-AI] Échec IA, fallback heuristique: {}", e.getMessage());
            return heuristicInsight(req, advanced);
        }
    }

    private void normalize(CoachInsightResponse r, boolean advanced) {
        if (r.getGlobalRisk() == null) {
            r.setGlobalRisk(CoachInsightResponse.GlobalRisk.builder().score(50).level("MEDIUM").summary("Analyse indisponible").build());
        }
        if (r.getCauses() == null) r.setCauses(new ArrayList<>());
        if (r.getActions() == null) r.setActions(new ArrayList<>());
        while (r.getCauses().size() < 3) {
            r.getCauses().add(CoachInsightResponse.CauseItem.builder()
                    .title("Cause " + (r.getCauses().size() + 1))
                    .detail("À affiner avec plus de données mission.")
                    .impact(5)
                    .build());
        }
        while (r.getActions().size() < 3) {
            r.getActions().add(CoachInsightResponse.ActionItem.builder()
                    .title("Action " + (r.getActions().size() + 1))
                    .detail("Clarifier le périmètre et les livrables avec le sous-traitant.")
                    .priority("THIS_WEEK")
                    .expectedRiskReduction(5)
                    .build());
        }
        if (r.getExpectedImpact() == null) {
            r.setExpectedImpact(CoachInsightResponse.ExpectedImpact.builder()
                    .riskReductionIfApplied(10)
                    .newEstimatedScore(Math.max(0, r.getGlobalRisk().getScore() - 10))
                    .confidenceLevel("MEDIUM")
                    .build());
        }
        if (r.getProfessionalTip() == null || r.getProfessionalTip().isBlank()) {
            r.setProfessionalTip("Formalisez critères d'acceptation et jalons intermédiaires avant signature.");
        }
        if (r.getUrgencyVerdict() == null) r.setUrgencyVerdict("REVIEW_FIRST");
        r.setCoachSignature("Coach IA • " + Instant.now());
        if (advanced && r.getWhatIf() == null) {
            r.setWhatIf(Map.of("scenario", "Budget +10% et délai +5 jours", "riskDelta", -8));
        }
    }

    private CoachInsightResponse heuristicInsight(CoachInsightRequest req, boolean advanced) {
        int score = 45;
        if (req.getBudget() != null && req.getBudget().doubleValue() < 2500) score += 15;
        if (req.getDurationDays() != null && req.getDurationDays() < 10) score += 10;
        if (req.getScope() == null || req.getScope().trim().length() < 40) score += 12;
        score = Math.min(100, score);
        String level = score >= 71 ? "HIGH" : score >= 41 ? "MEDIUM" : "LOW";
        CoachInsightResponse.GlobalRisk gr = CoachInsightResponse.GlobalRisk.builder()
                .score(score)
                .level(level)
                .summary("Estimation locale (sans clé Anthropic) basée sur budget, délai et clarté du périmètre.")
                .build();
        List<CoachInsightResponse.CauseItem> causes = List.of(
                CoachInsightResponse.CauseItem.builder().title("Visibilité budget").detail("Le budget peut être serré vs complexité.").impact(7).build(),
                CoachInsightResponse.CauseItem.builder().title("Délai").detail("Les délais courts augmentent le risque opérationnel.").impact(6).build(),
                CoachInsightResponse.CauseItem.builder().title("Scope").detail("Un périmètre flou retarde la validation des livrables.").impact(6).build()
        );
        List<CoachInsightResponse.ActionItem> actions = List.of(
                CoachInsightResponse.ActionItem.builder().title("Renforcer le scope").detail("Ajouter critères d'acceptation et jalons.").priority("URGENT").expectedRiskReduction(10).build(),
                CoachInsightResponse.ActionItem.builder().title("Buffer budget").detail("Prévoir 10–15% de contingence.").priority("THIS_WEEK").expectedRiskReduction(8).build(),
                CoachInsightResponse.ActionItem.builder().title("Point mi-parcours").detail("Checkpoint à mi-temps pour ajuster.").priority("OPTIONAL").expectedRiskReduction(5).build()
        );
        CoachInsightResponse.ExpectedImpact ei = CoachInsightResponse.ExpectedImpact.builder()
                .riskReductionIfApplied(18)
                .newEstimatedScore(Math.max(0, score - 18))
                .confidenceLevel("MEDIUM")
                .build();
        Map<String, Object> whatIf = advanced
                ? Map.of("scenario", "+10% budget, +5 jours", "riskDelta", -10, "note", "Simulation locale")
                : Map.of("scenario", "n/a", "riskDelta", 0);
        return CoachInsightResponse.builder()
                .globalRisk(gr)
                .causes(new ArrayList<>(causes))
                .actions(new ArrayList<>(actions))
                .expectedImpact(ei)
                .professionalTip("Mieux vaut négocier le scope que compresser le prix sans marge.")
                .urgencyVerdict(score >= 70 ? "REVIEW_FIRST" : "PROCEED")
                .coachSignature("Coach IA • " + Instant.now())
                .whatIf(whatIf)
                .build();
    }

    private String callClaude(String userContent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", anthropicModel);
        body.put("max_tokens", 4096);
        body.put("system", SYSTEM_PROMPT);
        body.put("messages", List.of(Map.of("role", "user", "content", userContent)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(anthropicUrl, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("HTTP " + resp.getStatusCode());
        }
        JsonNode root = objectMapper.readTree(resp.getBody());
        String text = root.at("/content/0/text").asText("");
        if (text.isBlank()) throw new IllegalStateException("Texte IA vide");
        return text;
    }

    private static String extractJsonObject(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int start = t.indexOf('{');
            int end = t.lastIndexOf('}');
            if (start >= 0 && end > start) return t.substring(start, end + 1);
        }
        int a = t.indexOf('{');
        int b = t.lastIndexOf('}');
        if (a >= 0 && b > a) return t.substring(a, b + 1);
        return t;
    }
}
