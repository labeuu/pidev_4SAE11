package org.example.subcontracting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.dto.request.SubcontractRiskCockpitRequest;
import org.example.subcontracting.dto.response.*;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SubcontractRiskCockpitService {

    private final SubcontractRepository subcontractRepository;

    public SubcontractRiskCockpitResponse analyze(SubcontractRiskCockpitRequest req) {
        var gauges = buildGauges(req);
        int total = gauges.stream().mapToInt(g -> g.getScore() != null ? g.getScore() : 0).sum() / Math.max(1, gauges.size());
        String level = riskLevel(total);

        List<RiskRecommendationDto> recs = buildRecommendations(req, gauges, total);
        List<RiskAlternativeDto> alternatives = total > 80 ? buildAlternatives(req, total) : List.of();

        String narrative = "Analyse en direct: risque global " + total + "/100 (" + level + ").";

        return SubcontractRiskCockpitResponse.builder()
                .totalRiskScore(total)
                .level(level)
                .streamedNarrative(narrative)
                .gauges(gauges)
                .recommendations(recs)
                .alternatives(alternatives)
                .build();
    }

    public SseEmitter stream(SubcontractRiskCockpitRequest req) {
        SseEmitter emitter = new SseEmitter(30000L);
        new Thread(() -> {
            try {
                var resp = analyze(req);
                emitter.send(SseEmitter.event().name("chunk").data("Analyse du budget..."));
                Thread.sleep(120);
                emitter.send(SseEmitter.event().name("chunk").data("Analyse du délai..."));
                Thread.sleep(120);
                emitter.send(SseEmitter.event().name("chunk").data("Analyse qualité/relation/marché..."));
                Thread.sleep(120);
                emitter.send(SseEmitter.event().name("result").data(resp));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    private List<RiskGaugeDto> buildGauges(SubcontractRiskCockpitRequest req) {
        int budgetRisk = computeBudgetRisk(req.getBudget());
        int durationRisk = computeDurationRisk(days(req.getStartDate(), req.getDeadline()));
        int qualityRisk = computeQualityRisk(req.getScope(), req.getRequiredSkills());
        int relationRisk = computeRelationRisk(req.getMainFreelancerId(), req.getSubcontractorId());
        int marketRisk = computeMarketRisk(req.getRequiredSkills());

        return List.of(
                gauge("BUDGET", "Risque budgétaire", budgetRisk, "Budget proposé par rapport à une zone saine 300-2000 TND."),
                gauge("DEADLINE", "Risque délai", durationRisk, "Durée de mission: plus elle est courte, plus le risque augmente."),
                gauge("QUALITY", "Risque qualité", qualityRisk, "Scope vague ou compétences manquantes augmentent le risque."),
                gauge("RELATION", "Risque relationnel", relationRisk, "Basé sur historique de collaboration entre principal/sous-traitant."),
                gauge("MARKET", "Risque marché", marketRisk, "Compétences très demandées = tension marché potentielle.")
        );
    }

    private RiskGaugeDto gauge(String key, String label, int score, String explanation) {
        return RiskGaugeDto.builder().key(key).label(label).score(score).level(riskLevel(score)).explanation(explanation).build();
    }

    private int computeBudgetRisk(BigDecimal budget) {
        if (budget == null) return 60;
        if (budget.compareTo(BigDecimal.valueOf(200)) < 0) return 85;
        if (budget.compareTo(BigDecimal.valueOf(300)) < 0) return 70;
        if (budget.compareTo(BigDecimal.valueOf(2000)) <= 0) return 35;
        if (budget.compareTo(BigDecimal.valueOf(4000)) <= 0) return 60;
        return 78;
    }

    private int computeDurationRisk(long days) {
        if (days <= 0) return 88;
        if (days < 7) return 80;
        if (days < 14) return 62;
        if (days <= 45) return 35;
        if (days <= 90) return 48;
        return 60;
    }

    private int computeQualityRisk(String scope, List<String> skills) {
        int risk = 40;
        if (scope == null || scope.trim().length() < 30) risk += 25;
        if (skills == null || skills.isEmpty()) risk += 20;
        else if (skills.size() < 2) risk += 10;
        return clamp(risk);
    }

    private int computeRelationRisk(Long mainId, Long subId) {
        if (mainId == null || subId == null) return 55;
        long collabs = subcontractRepository.countByMainFreelancerIdAndSubcontractorId(mainId, subId);
        if (collabs >= 5) return 25;
        if (collabs >= 2) return 38;
        if (collabs == 1) return 50;
        return 68;
    }

    private int computeMarketRisk(List<String> skills) {
        if (skills == null || skills.isEmpty()) return 58;
        int highDemand = 0;
        for (String s : skills) {
            String k = s == null ? "" : s.toLowerCase(Locale.ROOT);
            if (k.contains("react") || k.contains("angular") || k.contains("ai") || k.contains("docker")) {
                highDemand++;
            }
        }
        int r = 35 + highDemand * 9;
        return clamp(r);
    }

    private List<RiskRecommendationDto> buildRecommendations(SubcontractRiskCockpitRequest req, List<RiskGaugeDto> gauges, int total) {
        List<RiskRecommendationDto> out = new ArrayList<>();
        RiskGaugeDto b = byKey(gauges, "BUDGET");
        RiskGaugeDto d = byKey(gauges, "DEADLINE");
        RiskGaugeDto q = byKey(gauges, "QUALITY");
        if (b != null && b.getScore() > 65) {
            out.add(RiskRecommendationDto.builder()
                    .text("Réduire le budget de 15% pour retrouver une marge plus viable.")
                    .action(RiskRecommendationActionDto.builder().type("ADJUST_BUDGET").budgetMultiplier(0.85).build())
                    .build());
        }
        if (d != null && d.getScore() > 65) {
            out.add(RiskRecommendationDto.builder()
                    .text("Allonger la durée de 7 jours pour réduire le risque de retard.")
                    .action(RiskRecommendationActionDto.builder().type("ADJUST_DURATION").durationDeltaDays(7).build())
                    .build());
        }
        if (q != null && q.getScore() > 60) {
            out.add(RiskRecommendationDto.builder()
                    .text("Préciser le scope avec des livrables mesurables pour limiter les risques qualité.")
                    .action(RiskRecommendationActionDto.builder().type("REFINE_SCOPE").scopeHint("Ajouter livrables, critères d'acceptation, jalons hebdomadaires.").build())
                    .build());
        }
        if (total > 80) {
            out.add(RiskRecommendationDto.builder()
                    .text("Risque critique: privilégier une configuration alternative avant soumission.")
                    .action(RiskRecommendationActionDto.builder().type("OPEN_ALTERNATIVES").build())
                    .build());
        }
        if (out.isEmpty()) {
            out.add(RiskRecommendationDto.builder()
                    .text("Risque maîtrisé: conserver les paramètres et ajouter un point de contrôle intermédiaire.")
                    .action(RiskRecommendationActionDto.builder().type("NO_OP").build())
                    .build());
        }
        return out;
    }

    private List<RiskAlternativeDto> buildAlternatives(SubcontractRiskCockpitRequest req, int score) {
        List<RiskAlternativeDto> out = new ArrayList<>();
        BigDecimal budget = req.getBudget() != null ? req.getBudget() : BigDecimal.ZERO;
        out.add(RiskAlternativeDto.builder().label("Alternative A").score(Math.max(20, score - 18)).changes("Budget -20%, durée +7j").build());
        out.add(RiskAlternativeDto.builder().label("Alternative B").score(Math.max(25, score - 12)).changes("Scope réduit de 30%, compétences ciblées").build());
        out.add(RiskAlternativeDto.builder().label("Alternative C").score(Math.max(30, score - 10)).changes("Budget " + budget.multiply(BigDecimal.valueOf(0.9)).setScale(2, RoundingMode.HALF_UP) + " + revue hebdo").build());
        return out;
    }

    private static long days(String start, String end) {
        try {
            if (start == null || end == null || start.isBlank() || end.isBlank()) return -1;
            return ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end));
        } catch (Exception e) {
            return -1;
        }
    }

    private static RiskGaugeDto byKey(List<RiskGaugeDto> list, String key) {
        return list.stream().filter(x -> key.equals(x.getKey())).findFirst().orElse(null);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String riskLevel(int score) {
        if (score > 80) return "CRITICAL";
        if (score > 60) return "HIGH";
        if (score > 35) return "MEDIUM";
        return "LOW";
    }
}
