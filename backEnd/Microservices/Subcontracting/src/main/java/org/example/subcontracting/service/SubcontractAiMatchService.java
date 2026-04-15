package org.example.subcontracting.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.PortfolioFeignClient;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.ExperienceRestDto;
import org.example.subcontracting.client.dto.PortfolioSkillDto;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.SubcontractMatchCandidateDto;
import org.example.subcontracting.dto.response.SubcontractMatchResponse;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
@Slf4j
public class SubcontractAiMatchService {

    private final UserFeignClient userFeignClient;
    private final PortfolioFeignClient portfolioFeignClient;
    private final SubcontractRepository subcontractRepository;
    private final SubcontractDashboardService dashboardService;
    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.api.model:claude-3-5-haiku-20241022}")
    private String anthropicModel;

    @Value("${anthropic.api.url:https://api.anthropic.com/v1/messages}")
    private String anthropicUrl;

    @Autowired
    public SubcontractAiMatchService(
            UserFeignClient userFeignClient,
            PortfolioFeignClient portfolioFeignClient,
            SubcontractRepository subcontractRepository,
            SubcontractDashboardService dashboardService,
            @Qualifier("anthropicRestTemplate") RestTemplate restTemplate) {
        this.userFeignClient = userFeignClient;
        this.portfolioFeignClient = portfolioFeignClient;
        this.subcontractRepository = subcontractRepository;
        this.dashboardService = dashboardService;
        this.restTemplate = restTemplate;
    }

    public SubcontractMatchResponse matchSubcontractors(Long mainFreelancerId, List<String> requiredSkills) {
        if (mainFreelancerId == null || mainFreelancerId <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "mainFreelancerId invalide");
        }
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Au moins une compétence est requise");
        }
        List<String> skills = requiredSkills.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (skills.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Au moins une compétence est requise");
        }
        List<UserRemoteDto> freelancers;
        try {
            freelancers = userFeignClient.getAllUsers();
        } catch (Exception e) {
            log.error("User service indisponible pour le matching IA", e);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Service utilisateurs indisponible");
        }

        List<UserRemoteDto> candidates = freelancers.stream()
                .filter(u -> u != null && u.getId() != null)
                .filter(u -> "FREELANCER".equalsIgnoreCase(safeRole(u)))
                .filter(u -> !mainFreelancerId.equals(u.getId()))
                .filter(u -> u.getIsActive() == null || Boolean.TRUE.equals(u.getIsActive()))
                .toList();

        Map<Long, EnrichedProfile> enriched = new LinkedHashMap<>();
        for (UserRemoteDto u : candidates) {
            try {
                enriched.put(u.getId(), buildProfile(mainFreelancerId, u));
            } catch (Exception ex) {
                log.warn("Profil incomplet pour freelancer {}: {}", u.getId(), ex.getMessage());
            }
        }
        if (enriched.isEmpty()) {
            return SubcontractMatchResponse.builder().candidates(List.of()).build();
        }

        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            log.warn("[SUBCONTRACT-AI] Aucune clé Anthropic configurée — matching par compétences (hors LLM). "
                    + "Pour activer l’IA, définissez la variable d’environnement ANTHROPIC_API_KEY ou anthropic.api.key.");
            return SubcontractMatchResponse.builder().candidates(heuristicMatch(skills, enriched)).build();
        }

        String userPrompt = buildUserPrompt(skills, enriched);
        String rawJson;
        try {
            rawJson = callClaude(userPrompt);
        } catch (Exception e) {
            log.error("Appel Anthropic échoué", e);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Échec de l'analyse IA : " + e.getMessage());
        }

        ClaudeMatchEnvelope envelope;
        try {
            envelope = parseClaudeJson(rawJson);
        } catch (Exception e) {
            log.error("Réponse IA non JSON: {}", rawJson);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Réponse IA invalide");
        }

        if (envelope.matches == null) {
            return SubcontractMatchResponse.builder().candidates(heuristicMatch(skills, enriched)).build();
        }

        Map<Long, SubcontractMatchCandidateDto> byId = new LinkedHashMap<>();
        for (EnrichedProfile p : enriched.values()) {
            byId.put(p.userId(), buildHeuristicCandidate(skills, p));
        }

        for (ClaudeMatchRow row : envelope.matches) {
            if (row == null || row.freelancerId == null) continue;
            EnrichedProfile prof = enriched.get(row.freelancerId);
            if (prof == null) continue;
            SubcontractMatchCandidateDto baseline = byId.get(row.freelancerId);
            int score = row.matchScore != null ? row.matchScore : (baseline != null ? baseline.getMatchScore() : 0);
            score = Math.max(0, Math.min(100, score));

            List<String> reasons = row.matchReasons != null
                    ? row.matchReasons.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList()
                    : List.of();
            if (reasons.isEmpty() && baseline != null && baseline.getMatchReasons() != null) {
                reasons = baseline.getMatchReasons();
            }

            byId.put(row.freelancerId, SubcontractMatchCandidateDto.builder()
                    .freelancerId(prof.userId)
                    .fullName(prof.fullName)
                    .email(prof.email)
                    .matchScore(score)
                    .matchReasons(reasons.isEmpty() ? List.of("Évaluation IA") : reasons)
                    .trustScore(prof.trustScore)
                    .previousCollaborations(prof.previousCollaborations)
                    .recommendation(normalizeRecommendation(row.recommendation))
                    .build());
        }

        List<SubcontractMatchCandidateDto> out = new ArrayList<>(byId.values());
        out.sort(Comparator.comparing(SubcontractMatchCandidateDto::getMatchScore).reversed());
        return SubcontractMatchResponse.builder().candidates(out).build();
    }

    private static String safeRole(UserRemoteDto u) {
        return u.getRole() != null ? u.getRole() : "";
    }

    private EnrichedProfile buildProfile(Long mainFreelancerId, UserRemoteDto u) {
        Long uid = u.getId();
        String fullName = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                + (u.getLastName() != null ? u.getLastName() : "")).trim();
        if (fullName.isEmpty()) fullName = "Freelancer #" + uid;

        List<PortfolioSkillDto> skillList = safeList(() -> portfolioFeignClient.getSkillsByUserId(uid));
        String skillsText = skillList.stream()
                .map(PortfolioSkillDto::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
        if (skillsText.isEmpty()) skillsText = "(aucune compétence déclarée dans le portfolio)";

        List<ExperienceRestDto> exps = safeList(() -> portfolioFeignClient.getExperiencesByUserId(uid));
        String expText = exps.stream()
                .limit(8)
                .map(e -> {
                    String t = e.getTitle() != null ? e.getTitle() : "";
                    String c = e.getCompanyOrClientName() != null ? e.getCompanyOrClientName() : "";
                    return (t + " @ " + c).trim();
                })
                .filter(s -> !s.equals("@"))
                .collect(Collectors.joining(" | "));
        if (expText.isEmpty()) expText = "(aucune expérience renseignée)";

        int trust = dashboardService.computeScore(uid).getScore();
        long prev = subcontractRepository.countByMainFreelancerIdAndSubcontractorId(mainFreelancerId, uid);
        boolean active = u.getIsActive() == null || Boolean.TRUE.equals(u.getIsActive());

        return new EnrichedProfile(uid, fullName,
                u.getEmail() != null ? u.getEmail() : "",
                skillsText, expText, trust, prev, active);
    }

    private static <T> List<T> safeList(java.util.concurrent.Callable<List<T>> call) {
        try {
            List<T> l = call.call();
            return l != null ? l : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Si l’API Anthropic n’est pas configurée : score basé sur la présence des compétences demandées
     * dans le texte portfolio/expérience, le trust score et l’historique avec le principal.
     */
    private List<SubcontractMatchCandidateDto> heuristicMatch(List<String> requiredSkills,
                                                              Map<Long, EnrichedProfile> enriched) {
        List<String> req = requiredSkills.stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .filter(s -> !s.isEmpty())
                .toList();
        if (req.isEmpty()) {
            return List.of();
        }
        List<SubcontractMatchCandidateDto> out = new ArrayList<>();
        for (EnrichedProfile p : enriched.values()) {
            out.add(buildHeuristicCandidate(requiredSkills, p));
        }
        out.sort(Comparator.comparing(SubcontractMatchCandidateDto::getMatchScore).reversed());
        return out;
    }

    /**
     * Score de base pour chaque profil (toujours inclus). Si aucune compétence ne correspond,
     * le score repose surtout sur confiance plateforme et collaborations passées.
     */
    private SubcontractMatchCandidateDto buildHeuristicCandidate(List<String> requiredSkills, EnrichedProfile p) {
        List<String> originals = new ArrayList<>();
        List<String> reqLower = new ArrayList<>();
        for (String s : requiredSkills) {
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty()) continue;
            originals.add(t);
            reqLower.add(t.toLowerCase(Locale.ROOT));
        }
        int nReq = reqLower.size();
        String hay = (p.skillsText() + " " + p.experienceText()).toLowerCase(Locale.ROOT);
        List<String> reasons = new ArrayList<>();
        int hits = 0;
        for (int i = 0; i < reqLower.size(); i++) {
            String r = reqLower.get(i);
            if (hay.contains(r)) {
                hits++;
                if (reasons.size() < 4) {
                    reasons.add("Correspondance sur la compétence : " + originals.get(i));
                }
            }
        }
        if (hits == 0) {
            reasons.add("Aucune correspondance directe sur les compétences demandées");
        }
        int matchPoints = nReq > 0 ? (int) Math.round((100.0 * hits) / nReq) : 0;
        int trustBonus = Math.min(12, p.trustScore() / 8);
        int collabBonus = (int) Math.min(10, p.previousCollaborations() * 3);
        int score = Math.min(100, matchPoints + trustBonus + collabBonus);
        String rec;
        if (score >= 85) {
            rec = "HIGHLY_RECOMMENDED";
        } else if (score >= 72) {
            rec = "RECOMMENDED";
        } else {
            rec = "POSSIBLE";
        }
        return SubcontractMatchCandidateDto.builder()
                .freelancerId(p.userId())
                .fullName(p.fullName())
                .email(p.email())
                .matchScore(score)
                .matchReasons(reasons)
                .trustScore(p.trustScore())
                .previousCollaborations(p.previousCollaborations())
                .recommendation(rec)
                .build();
    }

    private String buildUserPrompt(List<String> requiredSkills, Map<Long, EnrichedProfile> profiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Compétences requises pour la sous-traitance : ")
                .append(String.join(", ", requiredSkills))
                .append("\n\n");
        sb.append("Pour chaque freelancer ci-dessous, estime un score de correspondance 0-100 avec les compétences demandées, ")
                .append("en t'appuyant sur les compétences déclarées, l'historique d'expérience, le trustScore plateforme (performance sous-traitance), ")
                .append("le nombre de collaborations passées avec le freelancer principal, et la disponibilité (compte actif).\n\n");

        for (EnrichedProfile p : profiles.values()) {
            sb.append("- ID ").append(p.userId)
                    .append(" | ").append(p.fullName)
                    .append(" | email: ").append(p.email.isEmpty() ? "n/a" : p.email)
                    .append("\n  Compétences portfolio: ").append(p.skillsText)
                    .append("\n  Expériences: ").append(p.experienceText)
                    .append("\n  Trust score plateforme (0-100): ").append(p.trustScore)
                    .append("\n  Collaborations antérieures avec ce principal: ").append(p.previousCollaborations)
                    .append("\n  Compte actif / disponibilité: ").append(p.active ? "oui" : "non")
                    .append("\n\n");
        }

        sb.append("Réponds UNIQUEMENT avec un JSON valide de la forme suivante (sans markdown, sans texte autour) :\n");
        sb.append("{\n");
        sb.append("  \"matches\": [\n");
        sb.append("    {\n");
        sb.append("      \"freelancerId\": <long>,\n");
        sb.append("      \"matchScore\": <0-100>,\n");
        sb.append("      \"matchReasons\": [\"raison courte en français\", \"...\"],\n");
        sb.append("      \"recommendation\": \"HIGHLY_RECOMMENDED\" | \"RECOMMENDED\" | \"POSSIBLE\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("Inclus TOUS les freelancers listés ci-dessus dans \"matches\", chacun avec un matchScore entre 0 et 100. ")
                .append("Ne omets aucun ID : l’utilisateur doit voir tout le monde pour comparer et choisir.\n");
        return sb.toString();
    }

    private String callClaude(String userContent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", anthropicModel);
        body.put("max_tokens", 8192);
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
        String text = root.at("/content/0/text").asText(null);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Réponse Anthropic sans texte");
        }
        return text;
    }

    private ClaudeMatchEnvelope parseClaudeJson(String text) throws Exception {
        String json = extractJsonObject(text);
        return objectMapper.readValue(json, ClaudeMatchEnvelope.class);
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

    private static String normalizeRecommendation(String r) {
        if (r == null || r.isBlank()) return "POSSIBLE";
        String u = r.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (u.contains("HIGHLY")) return "HIGHLY_RECOMMENDED";
        if ("HIGHLY_RECOMMENDED".equals(u)) return "HIGHLY_RECOMMENDED";
        if ("RECOMMENDED".equals(u)) return "RECOMMENDED";
        if ("POSSIBLE".equals(u)) return "POSSIBLE";
        if (u.contains("RECOMMENDED") && !u.contains("HIGHLY")) return "RECOMMENDED";
        return "POSSIBLE";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ClaudeMatchEnvelope {
        public List<ClaudeMatchRow> matches;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ClaudeMatchRow {
        public Long freelancerId;
        public Integer matchScore;
        public List<String> matchReasons;
        public String recommendation;
    }

    private record EnrichedProfile(
            Long userId,
            String fullName,
            String email,
            String skillsText,
            String experienceText,
            int trustScore,
            long previousCollaborations,
            boolean active
    ) {}
}
