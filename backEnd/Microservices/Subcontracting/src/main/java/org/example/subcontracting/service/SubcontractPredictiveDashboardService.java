package org.example.subcontracting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.UserFeignClient;
import org.example.subcontracting.client.dto.UserRemoteDto;
import org.example.subcontracting.dto.response.MonthInsightDto;
import org.example.subcontracting.dto.response.PredictiveDashboardResponse;
import org.example.subcontracting.dto.response.RiskTrendPointDto;
import org.example.subcontracting.dto.response.SubcontractorInsightDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubcontractPredictiveDashboardService {

    private final SubcontractRepository subcontractRepository;
    private final SubcontractDeliverableRepository deliverableRepository;
    private final UserFeignClient userFeignClient;
    private final SubcontractEmailService subcontractEmailService;
    @Qualifier("anthropicRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.api.model:claude-3-5-haiku-20241022}")
    private String anthropicModel;

    @Value("${anthropic.api.url:https://api.anthropic.com/v1/messages}")
    private String anthropicUrl;

    public PredictiveDashboardResponse buildDashboard(Long mainFreelancerId) {
        List<Subcontract> all = subcontractRepository.findByMainFreelancerIdOrderByCreatedAtDesc(mainFreelancerId);
        LocalDateTime from = LocalDateTime.now().minusMonths(12);
        List<Subcontract> window = all.stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(from))
                .toList();

        Map<String, Double> successRate = successRateByCategory(window);
        List<SubcontractorInsightDto> allInsights = subcontractorInsights(window);
        List<SubcontractorInsightDto> topGood = allInsights.stream()
                .sorted(Comparator.comparing(SubcontractorInsightDto::getProfitabilityScore).reversed())
                .limit(3).toList();
        List<SubcontractorInsightDto> topRisk = allInsights.stream()
                .sorted(Comparator.comparing(SubcontractorInsightDto::getRiskScore).reversed())
                .limit(3).toList();

        List<MonthInsightDto> bestMonths = bestMonths(window);
        List<RiskTrendPointDto> trend = riskTrend(window);
        String nextIncident = predictNextIncident(window);
        String narrative = aiNarrative(successRate, topGood, topRisk, nextIncident);

        return PredictiveDashboardResponse.builder()
                .narrativeSummary(narrative)
                .successRateByCategory(successRate)
                .topProfitableSubcontractors(topGood)
                .topRiskySubcontractors(topRisk)
                .bestMonthsForSubcontracting(bestMonths)
                .riskTrend(trend)
                .nextIncidentPrediction(nextIncident)
                .monthlyReportHint("Rapport automatique envoyé le 1er de chaque mois (12 mois glissants).")
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Scheduled(cron = "0 0 8 1 * *")
    @Transactional
    public void sendMonthlyPredictiveReports() {
        List<Long> mainFreelancers = subcontractRepository.findAll().stream()
                .map(Subcontract::getMainFreelancerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        for (Long freelancerId : mainFreelancers) {
            try {
                PredictiveDashboardResponse dashboard = buildDashboard(freelancerId);
                subcontractEmailService.sendMonthlyPerformanceReport(freelancerId, dashboard);
                log.info("[PREDICTIVE] rapport mensuel envoyé freelancer={}", freelancerId);
            } catch (Exception e) {
                log.warn("[PREDICTIVE] échec envoi rapport freelancer={}: {}", freelancerId, e.getMessage());
            }
        }
    }

    private Map<String, Double> successRateByCategory(List<Subcontract> list) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (SubcontractCategory c : SubcontractCategory.values()) {
            List<Subcontract> byCat = list.stream().filter(s -> s.getCategory() == c).toList();
            long ok = byCat.stream().filter(s -> s.getStatus() == SubcontractStatus.COMPLETED || s.getStatus() == SubcontractStatus.CLOSED).count();
            double rate = byCat.isEmpty() ? 0.0 : (ok * 100.0 / byCat.size());
            out.put(c.name(), Math.round(rate * 10) / 10.0);
        }
        return out;
    }

    private List<SubcontractorInsightDto> subcontractorInsights(List<Subcontract> list) {
        Map<Long, List<Subcontract>> bySub = list.stream()
                .filter(s -> s.getSubcontractorId() != null)
                .collect(Collectors.groupingBy(Subcontract::getSubcontractorId));
        List<SubcontractorInsightDto> out = new ArrayList<>();
        for (var e : bySub.entrySet()) {
            Long subId = e.getKey();
            List<Subcontract> items = e.getValue();
            long success = items.stream().filter(s -> s.getStatus() == SubcontractStatus.COMPLETED || s.getStatus() == SubcontractStatus.CLOSED).count();
            long failed = items.stream().filter(s -> s.getStatus() == SubcontractStatus.REJECTED || s.getStatus() == SubcontractStatus.CANCELLED).count();
            long overdue = items.stream().filter(this::hasOverdueLikely).count();
            int profitability = clamp((int) Math.round(35 + (success * 18.0) - (failed * 12.0) - (overdue * 8.0)));
            int risk = clamp(100 - profitability + (int) Math.min(20, failed * 4));
            out.add(SubcontractorInsightDto.builder()
                    .name(userName(subId))
                    .profitabilityScore(profitability)
                    .riskScore(risk)
                    .note("Succès: " + success + ", incidents: " + (failed + overdue))
                    .build());
        }
        return out;
    }

    private List<MonthInsightDto> bestMonths(List<Subcontract> list) {
        Map<Integer, List<Subcontract>> byMonth = list.stream()
                .filter(s -> s.getCreatedAt() != null)
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().getMonthValue()));
        List<MonthInsightDto> out = new ArrayList<>();
        for (var e : byMonth.entrySet()) {
            long ok = e.getValue().stream().filter(s -> s.getStatus() == SubcontractStatus.COMPLETED || s.getStatus() == SubcontractStatus.CLOSED).count();
            double score = e.getValue().isEmpty() ? 0 : ok * 100.0 / e.getValue().size();
            String m = java.time.Month.of(e.getKey()).getDisplayName(TextStyle.FULL, Locale.FRENCH);
            out.add(MonthInsightDto.builder()
                    .month(capitalize(m))
                    .score(Math.round(score * 10) / 10.0)
                    .rationale(score >= 60 ? "Fenêtre favorable" : "Fenêtre prudente")
                    .build());
        }
        return out.stream()
                .sorted(Comparator.comparing(MonthInsightDto::getScore).reversed())
                .limit(4)
                .toList();
    }

    private List<RiskTrendPointDto> riskTrend(List<Subcontract> list) {
        Map<YearMonth, List<Subcontract>> byYm = list.stream()
                .filter(s -> s.getCreatedAt() != null)
                .collect(Collectors.groupingBy(s -> YearMonth.from(s.getCreatedAt())));
        return byYm.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    double avg = e.getValue().stream().mapToInt(this::riskForSubcontract).average().orElse(0);
                    return RiskTrendPointDto.builder()
                            .month(e.getKey().toString())
                            .avgRiskScore(Math.round(avg * 10) / 10.0)
                            .build();
                })
                .toList();
    }

    private String predictNextIncident(List<Subcontract> list) {
        LocalDate now = LocalDate.now();
        LocalDate in7 = now.plusDays(7);
        long likely = list.stream()
                .filter(s -> s.getStatus() == SubcontractStatus.ACCEPTED || s.getStatus() == SubcontractStatus.IN_PROGRESS || s.getStatus() == SubcontractStatus.PROPOSED)
                .flatMap(s -> deliverableRepository.findBySubcontractIdOrderByDeadlineAsc(s.getId()).stream())
                .filter(d -> d.getDeadline() != null && !d.getDeadline().isBefore(now) && !d.getDeadline().isAfter(in7))
                .filter(d -> d.getStatus() != DeliverableStatus.APPROVED)
                .count();
        if (likely == 0) {
            return "Aucun incident critique probable dans les 7 prochains jours.";
        }
        return likely + " livrable(s) présentent un risque de retard sous 7 jours. Recommander un jalon de contrôle immédiat.";
    }

    private int riskForSubcontract(Subcontract s) {
        int r = 35;
        if (s.getStatus() == SubcontractStatus.REJECTED || s.getStatus() == SubcontractStatus.CANCELLED) r += 35;
        if (hasOverdueLikely(s)) r += 20;
        if (s.getStatus() == SubcontractStatus.COMPLETED || s.getStatus() == SubcontractStatus.CLOSED) r -= 18;
        return clamp(r);
    }

    private boolean hasOverdueLikely(Subcontract s) {
        return deliverableRepository.findBySubcontractIdOrderByDeadlineAsc(s.getId()).stream()
                .anyMatch(d -> d.getDeadline() != null && LocalDate.now().isAfter(d.getDeadline()) && d.getStatus() != DeliverableStatus.APPROVED);
    }

    private String aiNarrative(Map<String, Double> successRate,
                               List<SubcontractorInsightDto> topGood,
                               List<SubcontractorInsightDto> topRisk,
                               String nextIncident) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            return "Analyse prédictive générée sans LLM: améliorez les catégories faibles et sécurisez les missions à risque.";
        }
        try {
            String prompt = "Génère un résumé mensuel de performance de sous-traitance en français (5-7 lignes max), ton professionnel. "
                    + "Données: successRateByCategory=" + successRate
                    + ", topProfitable=" + topGood
                    + ", topRisky=" + topRisk
                    + ", nextIncident=" + nextIncident
                    + ". Donne 3 décisions recommandées pour le mois prochain.";
            String text = callClaude(prompt);
            return text.length() > 1000 ? text.substring(0, 1000) : text;
        } catch (Exception e) {
            return "Résumé prédictif indisponible temporairement. Priorité: sécuriser les livrables proches échéance.";
        }
    }

    @SuppressWarnings("null")
    private String callClaude(String userContent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", anthropicModel);
        body.put("max_tokens", 2048);
        body.put("messages", List.of(Map.of("role", "user", "content", userContent)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(anthropicUrl, entity, String.class);
        String bodyText = resp.getBody();
        if (bodyText == null) {
            throw new IllegalStateException("Réponse Anthropic vide");
        }
        JsonNode root = objectMapper.readTree(bodyText);
        String text = root.at("/content/0/text").asText("");
        if (text == null || text.isBlank()) throw new IllegalStateException("Réponse vide");
        return text;
    }

    private String userName(Long userId) {
        try {
            UserRemoteDto u = userFeignClient.getUserById(userId);
            if (u != null) {
                String n = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).trim();
                if (!n.isBlank()) return n;
            }
        } catch (Exception ignored) {}
        return "Freelancer";
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return "";
        return s.substring(0, 1).toUpperCase(Locale.FRENCH) + s.substring(1);
    }
}

