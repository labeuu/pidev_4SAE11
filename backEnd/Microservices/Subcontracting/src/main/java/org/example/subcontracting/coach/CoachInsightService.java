package org.example.subcontracting.coach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.coach.dto.CoachInsightRequest;
import org.example.subcontracting.coach.dto.CoachInsightResponse;
import org.example.subcontracting.coach.dto.DebitResultResponse;
import org.example.subcontracting.coach.entity.CoachInsightHistory;
import org.example.subcontracting.coach.entity.CoachWallet;
import org.example.subcontracting.coach.exception.AlreadyUsedFreeInsightException;
import org.example.subcontracting.coach.repository.CoachInsightHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoachInsightService {

    private final CoachWalletService coachWalletService;
    private final CoachInsightAiService coachInsightAiService;
    private final CoachInsightPersistenceService persistenceService;
    private final CoachInsightHistoryRepository historyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoachInsightResponse initialInsight(Long userId, CoachInsightRequest req) {
        CoachWallet w = coachWalletService.getOrCreateWallet(userId);
        if (Boolean.TRUE.equals(w.getFirstFreeUsed())) {
            throw new AlreadyUsedFreeInsightException();
        }
        CoachInsightResponse resp = coachInsightAiService.generate(req, false);
        try {
            persistenceService.persistInitialFree(userId, req, resp);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation insight impossible", e);
        }
        resp.setFree(true);
        resp.setPointsSpent(0);
        return resp;
    }

    public CoachInsightResponse advancedInsight(Long userId, CoachInsightRequest req) {
        CoachFeatureCode code = req.getFeatureCode() != null
                ? req.getFeatureCode()
                : CoachFeatureCode.RISK_DEEP_ANALYSIS;
        if (code == CoachFeatureCode.INITIAL_FREE) {
            code = CoachFeatureCode.RISK_DEEP_ANALYSIS;
        }
        String meta;
        try {
            meta = objectMapper.writeValueAsString(Map.of(
                    "subcontractId", req.getSubcontractId(),
                    "feature", code.name()));
        } catch (JsonProcessingException e) {
            meta = "{}";
        }
        DebitResultResponse debit = coachWalletService.debitForFeature(userId, code, meta);
        CoachInsightResponse resp = coachInsightAiService.generate(req, true);
        resp.setFree(false);
        resp.setPointsSpent(debit.getPointsSpent());
        try {
            persistenceService.persistAdvanced(userId, req, code, debit.getPointsSpent(), resp);
        } catch (JsonProcessingException e) {
            log.error("[COACH] Historique insight avancé non enregistré: {}", e.getMessage());
        }
        enrichAdvancedUx(resp);
        return resp;
    }

    /** UX : recommandation de remplacement si risque élevé (analyse avancée). */
    private void enrichAdvancedUx(CoachInsightResponse resp) {
        if (resp.getGlobalRisk() == null || resp.getGlobalRisk().getScore() == null) {
            return;
        }
        if (resp.getGlobalRisk().getScore() <= 70) {
            return;
        }
        Map<String, Object> whatIf = resp.getWhatIf() != null
                ? new LinkedHashMap<>(resp.getWhatIf())
                : new LinkedHashMap<>();
        whatIf.put("similarMissionsNote",
                "Comparer avec des sous-traitances similaires sur la plateforme pour calibrer budget et délais.");
        whatIf.put("subcontractorReplacementHint",
                "Risque élevé : envisagez un profil avec davantage de missions validées sur un périmètre comparable.");
        resp.setWhatIf(whatIf);
    }

    public Page<CoachInsightHistory> myHistory(Long userId, Pageable pageable) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<CoachInsightHistory> adminUserHistory(Long userId, Pageable pageable) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
