package org.example.subcontracting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.dto.response.MyCoachingProfileResponse;
import org.example.subcontracting.entity.DeliverableStatus;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractCategory;
import org.example.subcontracting.entity.SubcontractStatus;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubcontractCoachingService {

    private final SubcontractRepository subcontractRepository;
    private final SubcontractDeliverableRepository deliverableRepository;
    @Qualifier("anthropicRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.api.model:claude-3-5-haiku-20241022}")
    private String anthropicModel;

    @Value("${anthropic.api.url:https://api.anthropic.com/v1/messages}")
    private String anthropicUrl;

    private final Map<Long, MyCoachingProfileResponse> profileCache = new ConcurrentHashMap<>();

    public MyCoachingProfileResponse getProfile(Long mainFreelancerId) {
        return profileCache.computeIfAbsent(mainFreelancerId, this::computeProfile);
    }

    public void refreshProfile(Long mainFreelancerId) {
        profileCache.put(mainFreelancerId, computeProfile(mainFreelancerId));
    }

    /** Synchronise régulièrement les profils de coaching (incluant les sous-traitances clôturées récemment). */
    @Scheduled(cron = "0 */20 * * * *")
    @Transactional
    public void refreshProfilesScheduler() {
        List<Long> ids = subcontractRepository.findAll().stream()
                .map(Subcontract::getMainFreelancerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        for (Long id : ids) {
            try {
                refreshProfile(id);
            } catch (Exception e) {
                log.debug("[COACH] Refresh échoué user={}: {}", id, e.getMessage());
            }
        }
    }

    private MyCoachingProfileResponse computeProfile(Long mainFreelancerId) {
        List<Subcontract> all = subcontractRepository.findByMainFreelancerIdOrderByCreatedAtDesc(mainFreelancerId);
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Subcontract> recent = all.stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(sixMonthsAgo))
                .toList();

        long completed = recent.stream().filter(s -> s.getStatus() == SubcontractStatus.COMPLETED || s.getStatus() == SubcontractStatus.CLOSED).count();
        long failed = recent.stream().filter(s -> s.getStatus() == SubcontractStatus.REJECTED || s.getStatus() == SubcontractStatus.CANCELLED).count();
        long vagueScope = recent.stream().filter(s -> s.getScope() == null || s.getScope().trim().length() < 35).count();
        long underBudgetDev = recent.stream()
                .filter(s -> s.getCategory() == SubcontractCategory.DEVELOPMENT)
                .filter(s -> s.getBudget() != null && s.getBudget().compareTo(BigDecimal.valueOf(2500)) < 0)
                .count();
        long overdue = recent.stream().filter(this::hasOverdueDeliverable).count();

        Map<String, Double> successByCat = new LinkedHashMap<>();
        for (SubcontractCategory c : SubcontractCategory.values()) {
            List<Subcontract> cat = recent.stream().filter(s -> s.getCategory() == c).toList();
            long ok = cat.stream().filter(s -> s.getStatus() == SubcontractStatus.COMPLETED || s.getStatus() == SubcontractStatus.CLOSED).count();
            double rate = cat.isEmpty() ? 0 : ok * 100.0 / cat.size();
            successByCat.put(c.name(), Math.round(rate * 10) / 10.0);
        }

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> patterns = new ArrayList<>();
        List<String> tips = new ArrayList<>();

        if (completed >= failed + 2) strengths.add("Bonne capacité de pilotage: taux de finalisation supérieur aux échecs.");
        if (vagueScope <= 1) strengths.add("Les scopes récents sont globalement bien définis.");
        if (successByCat.getOrDefault("DESIGN", 0.0) >= 80) strengths.add("Excellente exécution sur les missions DESIGN.");

        if (overdue >= 2) weaknesses.add("Choix de sous-traitants avec retards récurrents.");
        if (underBudgetDev >= 2) weaknesses.add("Budgets DEVELOPMENT fréquemment sous-estimés (< 2500 TND).");
        if (vagueScope >= 2) weaknesses.add("Livrables / scope parfois trop vagues, source de rejet.");
        if (failed > completed) weaknesses.add("Plus d'échecs que de missions clôturées sur 6 mois.");

        double devRate = successByCat.getOrDefault("DEVELOPMENT", 0.0);
        double designRate = successByCat.getOrDefault("DESIGN", 0.0);
        patterns.add("Succès DESIGN: " + designRate + "% vs DEVELOPMENT: " + devRate + "%.");
        patterns.add("Incidents de retard sur " + overdue + " mission(s) récente(s).");

        if (underBudgetDev >= 1) {
            tips.add("À la création: pour DEVELOPMENT, éviter les budgets < 2500 TND sur 30 jours sans buffer.");
        }
        if (overdue >= 1) {
            tips.add("À la sélection sous-traitant: vérifier le taux de retard historique avant confirmation.");
        }
        if (vagueScope >= 1) {
            tips.add("À la définition des livrables: inclure critères d'acceptation + jalons hebdomadaires.");
        }
        tips.addAll(aiTips(successByCat, overdue, underBudgetDev, vagueScope));

        int progress = clamp((int) Math.round(55 + completed * 6 - failed * 8 - overdue * 5 - vagueScope * 4));

        return MyCoachingProfileResponse.builder()
                .strengths(nonEmpty(strengths, "Profil en apprentissage: continuez à clôturer des missions pour affiner les forces."))
                .weaknesses(nonEmpty(weaknesses, "Aucune faiblesse critique détectée actuellement."))
                .patterns(nonEmpty(patterns, "Patterns insuffisants pour une tendance robuste."))
                .personalizedTips(nonEmpty(tips, "Conservez vos pratiques actuelles avec une revue risque avant soumission."))
                .progressScore(progress)
                .build();
    }

    private List<String> aiTips(Map<String, Double> successByCat, long overdue, long underBudgetDev, long vagueScope) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) return List.of();
        try {
            String prompt = "Tu es un coach IA en sous-traitance. Donne 3 conseils personnalisés courts en français "
                    + "pour un freelancer principal, format JSON strict: {\"tips\":[\"...\",\"...\",\"...\"]}. "
                    + "Contexte: successByCategory=" + successByCat
                    + ", overdueCount=" + overdue
                    + ", underBudgetDev=" + underBudgetDev
                    + ", vagueScope=" + vagueScope + ".";
            String raw = callClaude(prompt);
            String json = extractJson(raw);
            JsonNode n = objectMapper.readTree(json).path("tips");
            if (!n.isArray()) return List.of();
            List<String> out = new ArrayList<>();
            n.forEach(x -> { if (x.isTextual()) out.add(x.asText()); });
            return out.stream().filter(s -> s != null && !s.isBlank()).limit(3).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean hasOverdueDeliverable(Subcontract s) {
        return deliverableRepository.findBySubcontractIdOrderByDeadlineAsc(s.getId()).stream()
                .anyMatch(d -> d.getDeadline() != null
                        && d.getDeadline().isBefore(java.time.LocalDate.now())
                        && d.getStatus() != DeliverableStatus.APPROVED);
    }

    @SuppressWarnings("null")
    private String callClaude(String userContent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", anthropicModel);
        body.put("max_tokens", 1024);
        body.put("messages", List.of(Map.of("role", "user", "content", userContent)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(anthropicUrl, entity, String.class);
        String b = resp.getBody();
        if (b == null) throw new IllegalStateException("Réponse IA vide");
        JsonNode root = objectMapper.readTree(b);
        String text = root.at("/content/0/text").asText("");
        if (text.isBlank()) throw new IllegalStateException("Texte IA vide");
        return text;
    }

    private static String extractJson(String text) {
        String t = text.trim();
        int a = t.indexOf('{');
        int b = t.lastIndexOf('}');
        if (a >= 0 && b > a) return t.substring(a, b + 1);
        return t;
    }

    private static List<String> nonEmpty(List<String> list, String fallback) {
        return list.isEmpty() ? List.of(fallback) : list;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}

