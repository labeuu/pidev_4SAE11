package org.example.subcontracting.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.dto.response.*;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractCategory;
import org.example.subcontracting.entity.SubcontractStatus;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubcontractAiDecisionAssistantService {

    private final SubcontractRepository subcontractRepository;
    private final SubcontractAiMatchService aiMatchService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RiskScoreResponse riskScore(Long mainFreelancerId, Long subcontractId) {
        Subcontract sc = loadAndAuthorize(mainFreelancerId, subcontractId);
        List<AiDecisionReasonDto> reasons = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<AiDecisionReferenceDto> refs = buildReferences(sc);

        int score = 18;
        BigDecimal threshold = recommendedBudgetThreshold(sc.getCategory());
        if (sc.getBudget() == null) {
            score += 18;
            reasons.add(reason("MISSING_BUDGET", "Budget non défini pour la mission.", 0.20));
            suggestions.add("Définissez un budget cible avant sélection finale du sous-traitant.");
        } else if (sc.getBudget().compareTo(threshold) < 0) {
            BigDecimal gap = threshold.subtract(sc.getBudget());
            int impact = Math.min(22, Math.max(8, gap.divide(BigDecimal.valueOf(250), java.math.RoundingMode.HALF_UP).intValue()));
            score += impact;
            reasons.add(reason("LOW_BUDGET", "Budget inférieur au seuil recommandé pour la catégorie (" + threshold + ").", 0.24));
            suggestions.add("Augmentez le budget de 10-20% ou réduisez le scope contractuel.");
        }

        long durationDays = safeDurationDays(sc.getStartDate(), sc.getDeadline());
        if (durationDays <= 0) {
            score += 14;
            reasons.add(reason("NO_TIMELINE", "Planning incomplet (dates de début/fin manquantes ou invalides).", 0.16));
            suggestions.add("Définissez une timeline avec jalons hebdomadaires.");
        } else if (durationDays < 7) {
            score += 16;
            reasons.add(reason("TIGHT_DEADLINE", "Durée très courte (" + durationDays + " jours).", 0.19));
            suggestions.add("Ajoutez un buffer de 20-30% sur le délai prévu.");
        }

        String scope = sc.getScope() == null ? "" : sc.getScope().trim();
        if (scope.length() < 35) {
            score += 12;
            reasons.add(reason("VAGUE_SCOPE", "Scope trop court/ambigu, risque de mauvaise interprétation.", 0.14));
            suggestions.add("Ajoutez critères d’acceptation et livrables mesurables.");
        }

        List<Subcontract> history = subcontractRepository.findByMainFreelancerIdOrderByCreatedAtDesc(sc.getMainFreelancerId());
        long evaluated = history.stream().filter(this::isClosedDecision).count();
        long failures = history.stream().filter(this::isFailure).count();
        if (evaluated >= 4) {
            double failureRate = failures * 100.0 / evaluated;
            if (failureRate >= 35) {
                score += 13;
                reasons.add(reason("HISTORICAL_FAILURE_RATE", "Historique avec taux d’échec élevé (" + Math.round(failureRate) + "%).", 0.15));
                suggestions.add("Activez un contrôle intermédiaire obligatoire à mi-parcours.");
            }
        }

        if (sc.getSubcontractorId() != null) {
            long pastCollabs = history.stream()
                    .filter(x -> Objects.equals(x.getSubcontractorId(), sc.getSubcontractorId()))
                    .count();
            long pastIncidents = history.stream()
                    .filter(x -> Objects.equals(x.getSubcontractorId(), sc.getSubcontractorId()))
                    .filter(this::isFailure)
                    .count();
            if (pastCollabs >= 2 && pastIncidents >= 1) {
                score += 10;
                reasons.add(reason("SUBCONTRACTOR_INCIDENT_HISTORY", "Incidents passés détectés avec ce sous-traitant.", 0.12));
                suggestions.add("Ajoutez une clause de révision et un jalon de validation anticipé.");
            }
        }

        int finalScore = clamp(score);
        return RiskScoreResponse.builder()
                .subcontractId(sc.getId())
                .riskScore(finalScore)
                .riskLevel(toRiskLevel(finalScore))
                .reasons(reasons.isEmpty() ? List.of(reason("LOW_SIGNAL", "Peu de signaux de risque critiques.", 0.10)) : reasons)
                .suggestions(suggestions.isEmpty() ? List.of("Maintenez le suivi hebdomadaire standard.") : suggestions)
                .confidence(computeConfidence(reasons.size(), refs.size()))
                .references(refs)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public TrapDetectionResponse trapsDetected(Long mainFreelancerId, Long subcontractId) {
        Subcontract sc = loadAndAuthorize(mainFreelancerId, subcontractId);
        List<TrapDetectionResponse.TrapItem> traps = new ArrayList<>();

        if (sc.getBudget() == null || sc.getBudget().compareTo(BigDecimal.ZERO) <= 0) {
            traps.add(trap("BUDGET_UNDEFINED", "CRITICAL", "Budget absent ou invalide.", "Renseignez un budget positif avant validation."));
        }
        if (sc.getSubcontractorId() == null) {
            traps.add(trap("NO_SUBCONTRACTOR_SELECTED", "HIGH", "Aucun sous-traitant sélectionné.", "Lancez le matching IA puis sélectionnez un profil."));
        }
        if (sc.getScope() == null || sc.getScope().trim().length() < 35) {
            traps.add(trap("VAGUE_SCOPE", "HIGH", "Scope insuffisant pour piloter la mission.", "Ajoutez livrables, critères qualité, exclusions."));
        }
        long durationDays = safeDurationDays(sc.getStartDate(), sc.getDeadline());
        if (durationDays <= 0) {
            traps.add(trap("TIMELINE_MISSING", "MEDIUM", "Dates incohérentes ou absentes.", "Renseignez date début + deadline réalistes."));
        } else if (durationDays < 3) {
            traps.add(trap("UNREALISTIC_DEADLINE", "HIGH", "Deadline trop courte (" + durationDays + " jours).", "Allongez la durée ou réduisez le scope."));
        }

        BigDecimal threshold = recommendedBudgetThreshold(sc.getCategory());
        if (sc.getBudget() != null && sc.getBudget().compareTo(threshold) < 0) {
            traps.add(trap("UNDER_ESTIMATED_BUDGET", "HIGH", "Budget sous le seuil conseillé (" + threshold + ").", "Réévaluez le budget ou découpez la mission."));
        }

        if (extractSkills(sc).isEmpty()) {
            traps.add(trap("NO_REQUIRED_SKILLS", "MEDIUM", "Compétences requises non définies.", "Ajoutez au moins 3 compétences techniques/fonctionnelles."));
        }

        String overallSeverity = traps.stream()
                .map(TrapDetectionResponse.TrapItem::getSeverity)
                .reduce("LOW", this::maxSeverity);

        return TrapDetectionResponse.builder()
                .subcontractId(sc.getId())
                .overallSeverity(overallSeverity)
                .traps(traps)
                .confidence(computeConfidence(traps.size(), 2))
                .references(buildReferences(sc))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public FreelancerMatchDecisionResponse matchFreelancers(Long mainFreelancerId, Long subcontractId, int top) {
        Subcontract sc = loadAndAuthorize(mainFreelancerId, subcontractId);
        List<String> skills = extractSkills(sc);
        if (skills.isEmpty()) {
            skills = defaultSkillsByCategory(sc.getCategory());
        }
        if (skills.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune compétence exploitable pour le matching.");
        }

        SubcontractMatchResponse base = aiMatchService.matchSubcontractors(sc.getMainFreelancerId(), skills);
        List<FreelancerMatchDecisionResponse.CandidateDecision> candidates = (base.getCandidates() == null ? List.<SubcontractMatchCandidateDto>of() : base.getCandidates())
                .stream()
                .limit(Math.max(1, top))
                .map(c -> FreelancerMatchDecisionResponse.CandidateDecision.builder()
                        .freelancerId(c.getFreelancerId())
                        .fullName(c.getFullName())
                        .matchScore(c.getMatchScore())
                        .recommendation(c.getRecommendation())
                        .explanation(toExplanation(c))
                        .trustScore(c.getTrustScore())
                        .previousCollaborations(c.getPreviousCollaborations())
                        .riskPenalty(clamp(100 - safeInt(c.getMatchScore())))
                        .confidence(0.62 + (safeInt(c.getMatchScore()) / 300.0))
                        .build())
                .toList();

        String summary = candidates.isEmpty()
                ? "Aucun profil compatible trouvé pour le moment."
                : "Top " + candidates.size() + " profils triés par score de matching avec explications.";

        return FreelancerMatchDecisionResponse.builder()
                .subcontractId(sc.getId())
                .summary(summary)
                .candidates(candidates)
                .confidence(candidates.isEmpty() ? 0.45 : 0.78)
                .references(buildReferences(sc))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public FailurePredictionResponse predictFailure(Long mainFreelancerId, Long subcontractId) {
        RiskScoreResponse risk = riskScore(mainFreelancerId, subcontractId);
        TrapDetectionResponse traps = trapsDetected(mainFreelancerId, subcontractId);

        int base = Math.round(risk.getRiskScore() * 0.75f);
        int trapImpact = traps.getTraps().stream().mapToInt(t -> switch (t.getSeverity()) {
            case "CRITICAL" -> 12;
            case "HIGH" -> 8;
            case "MEDIUM" -> 4;
            default -> 1;
        }).sum();
        int probability = clamp(base + trapImpact);

        List<String> drivers = new ArrayList<>();
        risk.getReasons().stream().limit(2).forEach(r -> drivers.add(r.getMessage()));
        traps.getTraps().stream().limit(2).forEach(t -> drivers.add(t.getMessage()));
        if (drivers.isEmpty()) {
            drivers.add("Aucun facteur critique dominant détecté.");
        }

        List<String> mitigation = new ArrayList<>(risk.getSuggestions());
        traps.getTraps().stream().map(TrapDetectionResponse.TrapItem::getFixNow).limit(3).forEach(mitigation::add);
        mitigation = mitigation.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().limit(5).toList();

        return FailurePredictionResponse.builder()
                .subcontractId(subcontractId)
                .failureProbability(probability)
                .riskLevel(toRiskLevel(probability))
                .topDrivers(drivers.stream().distinct().limit(4).toList())
                .mitigationPlan(mitigation)
                .confidence((risk.getConfidence() + traps.getConfidence()) / 2.0)
                .references(risk.getReferences())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private Subcontract loadAndAuthorize(Long mainFreelancerId, Long subcontractId) {
        if (subcontractId == null || subcontractId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subcontractId invalide");
        }
        Subcontract sc = subcontractRepository.findById(subcontractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sous-traitance introuvable"));
        if (mainFreelancerId != null && !Objects.equals(sc.getMainFreelancerId(), mainFreelancerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à cette sous-traitance");
        }
        return sc;
    }

    private List<String> extractSkills(Subcontract sc) {
        String raw = sc.getRequiredSkillsJson();
        if (raw == null || raw.isBlank()) return List.of();
        try {
            List<String> arr = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            return arr.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
        } catch (Exception ignore) {
            return List.of();
        }
    }

    private List<String> defaultSkillsByCategory(SubcontractCategory category) {
        if (category == null) return List.of();
        return switch (category) {
            case DEVELOPMENT -> List.of("Java", "Spring Boot", "API", "Database");
            case DESIGN -> List.of("UI", "UX", "Figma");
            case TESTING -> List.of("Testing", "QA", "Automation");
            case CONTENT -> List.of("Copywriting", "SEO", "Communication");
            case CONSULTING -> List.of("Business Analysis", "Planning", "Delivery");
        };
    }

    private List<AiDecisionReferenceDto> buildReferences(Subcontract sc) {
        List<AiDecisionReferenceDto> refs = new ArrayList<>();
        refs.add(AiDecisionReferenceDto.builder()
                .type("subcontract")
                .id("SC-" + sc.getId())
                .label(sc.getTitle())
                .build());

        List<Subcontract> history = subcontractRepository.findByMainFreelancerIdOrderByCreatedAtDesc(sc.getMainFreelancerId());
        history.stream().limit(3).forEach(h -> refs.add(AiDecisionReferenceDto.builder()
                .type("history")
                .id("SC-" + h.getId())
                .label(h.getStatus() + " • " + (h.getTitle() == null ? "mission" : h.getTitle()))
                .build()));
        return refs;
    }

    private boolean isClosedDecision(Subcontract s) {
        return s.getStatus() == SubcontractStatus.COMPLETED
                || s.getStatus() == SubcontractStatus.CLOSED
                || s.getStatus() == SubcontractStatus.REJECTED
                || s.getStatus() == SubcontractStatus.CANCELLED;
    }

    private boolean isFailure(Subcontract s) {
        return s.getStatus() == SubcontractStatus.REJECTED || s.getStatus() == SubcontractStatus.CANCELLED;
    }

    private AiDecisionReasonDto reason(String code, String message, double weight) {
        return AiDecisionReasonDto.builder().code(code).message(message).weight(weight).build();
    }

    private TrapDetectionResponse.TrapItem trap(String code, String severity, String message, String fixNow) {
        return TrapDetectionResponse.TrapItem.builder()
                .code(code)
                .severity(severity)
                .message(message)
                .fixNow(fixNow)
                .build();
    }

    private String toExplanation(SubcontractMatchCandidateDto c) {
        List<String> reasons = c.getMatchReasons() == null ? List.of() : c.getMatchReasons();
        String reasonText = reasons.stream().limit(2).collect(Collectors.joining(" | "));
        if (reasonText.isBlank()) reasonText = "Profil évalué via matching IA.";
        return "Score " + safeInt(c.getMatchScore()) + "/100. " + reasonText;
    }

    private String toRiskLevel(int score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 70) return "HIGH";
        if (score >= 45) return "MEDIUM";
        return "LOW";
    }

    private BigDecimal recommendedBudgetThreshold(SubcontractCategory category) {
        if (category == null) return BigDecimal.valueOf(1200);
        return switch (category) {
            case DEVELOPMENT -> BigDecimal.valueOf(2500);
            case DESIGN -> BigDecimal.valueOf(1200);
            case TESTING -> BigDecimal.valueOf(900);
            case CONTENT -> BigDecimal.valueOf(700);
            case CONSULTING -> BigDecimal.valueOf(1100);
        };
    }

    private long safeDurationDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) return -1;
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    private double computeConfidence(int signalCount, int referenceCount) {
        double raw = 0.55 + (signalCount * 0.05) + (referenceCount * 0.02);
        return Math.max(0.45, Math.min(0.97, raw));
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String maxSeverity(String a, String b) {
        return severityRank(b) > severityRank(a) ? b : a;
    }

    private int severityRank(String s) {
        return switch (s) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }
}

