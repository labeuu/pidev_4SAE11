package org.example.subcontracting.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.client.ContractFeignClient;
import org.example.subcontracting.client.dto.ContractRemoteDto;
import org.example.subcontracting.dto.response.*;
import org.example.subcontracting.entity.DeliverableStatus;
import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractDeliverable;
import org.example.subcontracting.entity.SubcontractStatus;
import org.example.subcontracting.repository.SubcontractDeliverableRepository;
import org.example.subcontracting.repository.SubcontractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@Slf4j
@Transactional(readOnly = true)
public class SubcontractFinancialAnalysisService {

    private final SubcontractRepository subcontractRepository;
    private final SubcontractDeliverableRepository deliverableRepository;
    private final ContractFeignClient contractFeignClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.api.model:claude-3-5-haiku-20241022}")
    private String anthropicModel;

    @Value("${anthropic.api.url:https://api.anthropic.com/v1/messages}")
    private String anthropicUrl;

    @Autowired
    public SubcontractFinancialAnalysisService(
            SubcontractRepository subcontractRepository,
            SubcontractDeliverableRepository deliverableRepository,
            ContractFeignClient contractFeignClient,
            @Qualifier("anthropicRestTemplate") RestTemplate restTemplate) {
        this.subcontractRepository = subcontractRepository;
        this.deliverableRepository = deliverableRepository;
        this.contractFeignClient = contractFeignClient;
        this.restTemplate = restTemplate;
    }

    public SubcontractFinancialAnalysisResponse analyze(Long subcontractId, Long mainFreelancerId) {
        if (mainFreelancerId == null || mainFreelancerId <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "mainFreelancerId invalide");
        }
        Subcontract sc = subcontractRepository.findById(subcontractId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Sous-traitance introuvable"));
        if (!mainFreelancerId.equals(sc.getMainFreelancerId())) {
            throw new ResponseStatusException(FORBIDDEN, "Accès réservé au freelancer principal");
        }
        AnalysisSnapshot snap = buildSnapshot(sc);
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            log.warn("Financial analysis fallback: Anthropic API key not set (configure ANTHROPIC_API_KEY or anthropic.api.key)");
            return buildFallbackResponse(snap);
        }

        String prompt = buildPrompt(sc, snap);
        String rawJson;
        try {
            rawJson = callClaude(prompt);
        } catch (Exception e) {
            log.error("Anthropic financial analysis failed", e);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Échec de l'analyse IA : " + e.getMessage());
        }

        ClaudeFinancialAi ai;
        try {
            ai = parseClaudeJson(rawJson);
        } catch (Exception e) {
            log.error("Réponse IA non JSON: {}", rawJson);
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Réponse IA invalide");
        }

        return buildResponseFromSnapshot(
                snap,
                ai.rentabilityScore,
                ai.verdict != null ? parseVerdict(ai.verdict) : null,
                ai.marginRate,
                ai.estimatedRoi,
                ai.breakEvenThreshold,
                ai.recommendations
        );
    }

    private SubcontractFinancialAnalysisResponse buildFallbackResponse(AnalysisSnapshot snap) {
        int score = snap.heuristicScore();
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Analyse IA indisponible : score calculé via heuristiques locales.");
        recommendations.addAll(defaultRecommendations(snap, score));
        return buildResponseFromSnapshot(
                snap,
                score,
                verdictFromScore(score),
                snap.estimatedMarginRate(),
                snap.estimatedRoiPlaceholder(),
                snap.breakEvenHintPercent(),
                recommendations
        );
    }

    private SubcontractFinancialAnalysisResponse buildResponseFromSnapshot(
            AnalysisSnapshot snap,
            Integer scoreRaw,
            FinancialVerdict verdictRaw,
            Double marginRateRaw,
            Double estimatedRoiRaw,
            Double breakEvenThresholdRaw,
            List<String> recommendationsRaw
    ) {
        int score = clamp(scoreRaw != null ? scoreRaw : snap.heuristicScore(), 0, 100);
        FinancialVerdict verdict = verdictRaw != null ? verdictRaw : verdictFromScore(score);
        List<String> recs = recommendationsRaw != null && !recommendationsRaw.isEmpty()
                ? recommendationsRaw
                : defaultRecommendations(snap, score);

        return SubcontractFinancialAnalysisResponse.builder()
                .rentabilityScore(score)
                .verdict(verdict)
                .marginRate(marginRateRaw != null ? marginRateRaw : snap.estimatedMarginRate())
                .estimatedRoi(estimatedRoiRaw != null ? estimatedRoiRaw : snap.estimatedRoiPlaceholder())
                .breakEvenThreshold(breakEvenThresholdRaw != null ? breakEvenThresholdRaw : snap.breakEvenHintPercent())
                .recommendations(recs)
                .principalContractBudget(snap.principalBudget())
                .subcontractBudget(snap.subBudget())
                .remainingMarginForPrincipal(snap.remainingForPrincipal())
                .subcontractToPrincipalRatioPercent(snap.ratioPercent())
                .currency(snap.currency())
                .paymentHistorySummary(snap.historySummary())
                .financialTimeline(snap.timeline())
                .otherSubcontractsOnContractTotal(snap.otherOnContractTotal())
                .build();
    }

    private AnalysisSnapshot buildSnapshot(Subcontract sc) {
        BigDecimal principal = null;
        if (sc.getContractId() != null) {
            try {
                ContractRemoteDto c = contractFeignClient.getContractById(sc.getContractId());
                if (c != null && c.getAmount() != null) {
                    principal = c.getAmount();
                }
            } catch (Exception e) {
                log.warn("Contrat {} indisponible: {}", sc.getContractId(), e.getMessage());
            }
        }

        BigDecimal subBudget = sc.getBudget() != null ? sc.getBudget() : BigDecimal.ZERO;
        String currency = sc.getCurrency() != null ? sc.getCurrency() : "TND";

        List<Subcontract> onContract = sc.getContractId() == null
                ? List.of()
                : subcontractRepository.findByContractId(sc.getContractId());

        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal others = BigDecimal.ZERO;
        for (Subcontract x : onContract) {
            if (!countsTowardAllocation(x.getStatus())) continue;
            BigDecimal b = x.getBudget() != null ? x.getBudget() : BigDecimal.ZERO;
            totalAllocated = totalAllocated.add(b);
            if (!x.getId().equals(sc.getId())) {
                others = others.add(b);
            }
        }

        BigDecimal remaining = null;
        Double ratio = null;
        if (principal != null && principal.compareTo(BigDecimal.ZERO) > 0) {
            remaining = principal.subtract(totalAllocated);
            ratio = subBudget.multiply(BigDecimal.valueOf(100))
                    .divide(principal, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        List<Subcontract> past = subcontractRepository.findBySubcontractorIdOrderByCreatedAtDesc(sc.getSubcontractorId())
                .stream()
                .filter(s -> !s.getId().equals(sc.getId()))
                .limit(10)
                .collect(Collectors.toList());

        int late = 0;
        int cancelledRej = 0;
        List<MissionFinanceRowDto> rows = new ArrayList<>();
        List<FinancialTimelineEntryDto> timeline = new ArrayList<>();

        for (Subcontract p : past) {
            boolean lateDel = hasLateDeliverables(p);
            if (lateDel) late++;
            if (p.getStatus() == SubcontractStatus.CANCELLED || p.getStatus() == SubcontractStatus.REJECTED) {
                cancelledRej++;
            }
            rows.add(MissionFinanceRowDto.builder()
                    .subcontractId(p.getId())
                    .title(p.getTitle())
                    .budget(p.getBudget())
                    .status(p.getStatus().name())
                    .hadLateDeliverables(lateDel)
                    .build());
            if (p.getCreatedAt() != null) {
                timeline.add(FinancialTimelineEntryDto.builder()
                        .date(p.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .label(truncate(p.getTitle(), 40))
                        .amount(p.getBudget())
                        .build());
            }
        }

        Collections.reverse(timeline);

        PaymentHistorySummaryDto hist = PaymentHistorySummaryDto.builder()
                .pastMissionsConsidered(past.size())
                .missionsWithLateDeliverables(late)
                .cancelledOrRejectedMissions(cancelledRej)
                .refundLikeEvents(0)
                .recentMissions(rows)
                .build();

        return new AnalysisSnapshot(
                principal,
                subBudget,
                remaining,
                ratio,
                currency,
                others,
                hist,
                timeline,
                past.size(),
                late,
                cancelledRej,
                principal != null && principal.compareTo(BigDecimal.ZERO) > 0
                        ? totalAllocated.multiply(BigDecimal.valueOf(100)).divide(principal, 2, RoundingMode.HALF_UP).doubleValue()
                        : null
        );
    }

    private static boolean countsTowardAllocation(SubcontractStatus s) {
        return s == SubcontractStatus.PROPOSED
                || s == SubcontractStatus.ACCEPTED
                || s == SubcontractStatus.IN_PROGRESS
                || s == SubcontractStatus.COMPLETED
                || s == SubcontractStatus.CLOSED;
    }

    private boolean hasLateDeliverables(Subcontract sc) {
        List<SubcontractDeliverable> dels = deliverableRepository.findBySubcontractIdOrderByDeadlineAsc(sc.getId());
        for (SubcontractDeliverable d : dels) {
            if (d.getDeadline() != null && LocalDate.now().isAfter(d.getDeadline())
                    && d.getStatus() != DeliverableStatus.APPROVED) {
                return true;
            }
        }
        return false;
    }

    private String buildPrompt(Subcontract sc, AnalysisSnapshot snap) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un analyste financier pour plateforme freelance. Réponds UNIQUEMENT par un objet JSON valide, sans markdown.\n");
        sb.append("Schéma exact :\n{\n");
        sb.append("  \"rentabilityScore\": <0-100 entier>,\n");
        sb.append("  \"verdict\": \"EXCELLENT_CHOICE\" | \"GOOD_CHOICE\" | \"RISKY\" | \"NOT_RECOMMENDED\",\n");
        sb.append("  \"marginRate\": <double, marge % estimée restant au principal après cette ST>,\n");
        sb.append("  \"estimatedRoi\": <double, ROI attendu % ou 0 si inconnu>,\n");
        sb.append("  \"breakEvenThreshold\": <double, ex. part max % du contrat à ne pas dépasser pour rester viable>,\n");
        sb.append("  \"recommendations\": [\"conseil concret en français\", ...]\n");
        sb.append("}\n\n");
        sb.append("Contexte agrégé (chiffres réels, ne pas inventer de montants) :\n");
        sb.append("- Titre sous-traitance: ").append(sc.getTitle()).append("\n");
        sb.append("- Budget sous-traitance: ").append(snap.subBudget()).append(" ").append(snap.currency()).append("\n");
        if (snap.principalBudget() != null) {
            sb.append("- Budget contrat principal: ").append(snap.principalBudget()).append(" ").append(snap.currency()).append("\n");
            sb.append("- Ratio ST/contrat: ").append(snap.ratioPercent() != null ? snap.ratioPercent() + "%" : "N/A").append("\n");
            sb.append("- Total autres ST sur ce contrat: ").append(snap.otherOnContractTotal()).append("\n");
            sb.append("- Marge résiduelle estimée pour le principal: ").append(snap.remainingForPrincipal()).append("\n");
        } else {
            sb.append("- Contrat principal: non lié ou montant indisponible.\n");
        }
        sb.append("- Historique sous-traitant: ").append(snap.pastCount()).append(" mission(s) passées analysées, ");
        sb.append(snap.lateCount()).append(" avec livrables en retard, ");
        sb.append(snap.cancelCount()).append(" annulée(s)/rejetée(s). Remboursements: non mesurés (0).\n");
        sb.append("Produis 4 à 8 recommandations actionnables en français (pénalités, renégociation budget, clause délai, etc.).\n");
        return sb.toString();
    }

    private String callClaude(String userContent) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", anthropicModel);
        body.put("max_tokens", 4096);
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
        var root = objectMapper.readTree(resp.getBody());
        String text = root.at("/content/0/text").asText(null);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Réponse Anthropic sans texte");
        }
        return text;
    }

    private ClaudeFinancialAi parseClaudeJson(String text) throws Exception {
        String json = extractJsonObject(text);
        return objectMapper.readValue(json, ClaudeFinancialAi.class);
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

    private static FinancialVerdict parseVerdict(String raw) {
        if (raw == null || raw.isBlank()) return FinancialVerdict.RISKY;
        String u = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return FinancialVerdict.valueOf(u);
        } catch (IllegalArgumentException e) {
            if (u.contains("EXCELLENT")) return FinancialVerdict.EXCELLENT_CHOICE;
            if (u.contains("GOOD")) return FinancialVerdict.GOOD_CHOICE;
            if (u.contains("NOT") || u.contains("NO_RECOMMENDED")) return FinancialVerdict.NOT_RECOMMENDED;
            return FinancialVerdict.RISKY;
        }
    }

    private static FinancialVerdict verdictFromScore(int score) {
        if (score >= 80) return FinancialVerdict.EXCELLENT_CHOICE;
        if (score >= 62) return FinancialVerdict.GOOD_CHOICE;
        if (score >= 38) return FinancialVerdict.RISKY;
        return FinancialVerdict.NOT_RECOMMENDED;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private List<String> defaultRecommendations(AnalysisSnapshot snap, int score) {
        List<String> out = new ArrayList<>();
        if (snap.ratioPercent() != null && snap.ratioPercent() > 60) {
            out.add("Le budget de cette sous-traitance représente une part élevée du contrat principal ; envisagez de renégocier ou de réduire le périmètre.");
        }
        if (snap.lateCount() > 0) {
            out.add("Le sous-traitant présente des retards sur livrables passés ; prévoyez des jalons intermédiaires et une clause de pénalité.");
        }
        if (snap.cancelCount() > 1) {
            out.add("Plusieurs missions passées annulées ou rejetées : documentez les critères d'acceptation avant signature.");
        }
        if (snap.remainingForPrincipal() != null && snap.remainingForPrincipal().compareTo(BigDecimal.ZERO) < 0) {
            out.add("La somme des sous-traitances dépasse le montant du contrat principal : risque financier majeur.");
        }
        if (out.isEmpty()) {
            out.add(score >= 60
                    ? "Surveillez la marge résiduelle tout au long de la mission."
                    : "Analysez le rapport qualité/prix et sécurisez les livrables par étapes.");
        }
        return out;
    }

    private record AnalysisSnapshot(
            BigDecimal principalBudget,
            BigDecimal subBudget,
            BigDecimal remainingForPrincipal,
            Double ratioPercent,
            String currency,
            BigDecimal otherOnContractTotal,
            PaymentHistorySummaryDto historySummary,
            List<FinancialTimelineEntryDto> timeline,
            int pastCount,
            int lateCount,
            int cancelCount,
            Double totalAllocatedPercentOfPrincipal
    ) {
        int heuristicScore() {
            int s = 72;
            if (ratioPercent != null) {
                if (ratioPercent > 70) s -= 28;
                else if (ratioPercent > 55) s -= 14;
                else if (ratioPercent > 45) s -= 6;
            }
            if (pastCount > 0) {
                double lateRatio = (double) lateCount / pastCount;
                s -= (int) (lateRatio * 25);
            }
            s -= Math.min(20, cancelCount * 6);
            if (remainingForPrincipal != null && principalBudget != null
                    && remainingForPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                s -= 25;
            }
            return Math.max(0, Math.min(100, s));
        }

        Double estimatedMarginRate() {
            if (remainingForPrincipal == null || principalBudget == null || principalBudget.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return remainingForPrincipal.multiply(BigDecimal.valueOf(100))
                    .divide(principalBudget, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        Double estimatedRoiPlaceholder() {
            if (ratioPercent == null) return null;
            return Math.max(0, 100 - ratioPercent) * 0.6;
        }

        Double breakEvenHintPercent() {
            if (ratioPercent == null) return 50.0;
            return Math.min(85, Math.max(35, 100 - ratioPercent * 0.5));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ClaudeFinancialAi {
        public Integer rentabilityScore;
        public String verdict;
        public Double marginRate;
        public Double estimatedRoi;
        public Double breakEvenThreshold;
        public List<String> recommendations;
    }
}
