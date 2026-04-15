package org.example.subcontracting.coach;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.coach.entity.CoachFeatureCost;
import org.example.subcontracting.coach.repository.CoachFeatureCostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoachFeatureCostsBootstrap implements CommandLineRunner {

    private final CoachFeatureCostRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        seed("RISK_DEEP_ANALYSIS", "Analyse de risque avancée", 1000,
                "Analyse approfondie des risques budgétaires, délais et qualité.");
        seed("MATCHING_ADVANCED", "Matching intelligent sous-traitant", 600,
                "Suggestion de profils alignés sur le besoin.");
        seed("DELIVERABLE_EVALUATION", "Évaluation qualité livrable", 400,
                "Revue structurée des livrables et critères d'acceptation.");
        seed("FRAUD_DETECTION", "Détection fraude et anomalies", 800,
                "Signaux d'anomalies contractuelles ou comportementales.");
        seed("POST_MORTEM", "Rapport post-mortem complet", 700,
                "Synthèse après mission : causes, impacts, leçons.");
        seed("NEGOTIATION_ASSIST", "Assistant de négociation IA", 500,
                "Appui à la négociation budget / scope / planning.");
        log.info("[COACH-WALLET] Coûts features coaching initialisés ({} entrées).", repository.count());
    }

    private void seed(String code, String label, int cost, String description) {
        repository.save(CoachFeatureCost.builder()
                .featureCode(code)
                .label(label)
                .costPoints(cost)
                .active(true)
                .description(description)
                .build());
    }
}
