package org.example.subcontracting.coach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.coach.dto.CoachInsightRequest;
import org.example.subcontracting.coach.dto.CoachInsightResponse;
import org.example.subcontracting.coach.entity.CoachInsightHistory;
import org.example.subcontracting.coach.repository.CoachInsightHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoachInsightPersistenceService {

    private final CoachWalletService coachWalletService;
    private final CoachInsightHistoryRepository historyRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void persistInitialFree(Long userId, CoachInsightRequest req, CoachInsightResponse resp)
            throws JsonProcessingException {
        String meta = objectMapper.writeValueAsString(Map.of(
                "subcontractId", req.getSubcontractId(),
                "kind", "INITIAL_FREE"));
        coachWalletService.saveInitialFreeConsumption(userId, meta);
        saveHistory(userId, req.getSubcontractId(), CoachFeatureCode.INITIAL_FREE.name(), true, 0, resp);
    }

    @Transactional
    public void persistAdvanced(Long userId, CoachInsightRequest req, CoachFeatureCode featureCode,
                                int pointsSpent, CoachInsightResponse resp) throws JsonProcessingException {
        saveHistory(userId, req.getSubcontractId(), featureCode.name(), false, pointsSpent, resp);
    }

    private void saveHistory(Long userId, Long subcontractId, String featureCode, boolean free,
                             int pointsSpent, CoachInsightResponse resp) throws JsonProcessingException {
        historyRepository.save(CoachInsightHistory.builder()
                .userId(userId)
                .subcontractId(subcontractId)
                .featureCode(featureCode)
                .free(free)
                .pointsSpent(pointsSpent)
                .insightResultJson(objectMapper.writeValueAsString(resp))
                .build());
    }
}
