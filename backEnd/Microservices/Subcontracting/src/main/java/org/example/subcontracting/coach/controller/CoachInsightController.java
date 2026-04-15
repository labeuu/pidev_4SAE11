package org.example.subcontracting.coach.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.coach.CoachInsightService;
import org.example.subcontracting.coach.dto.CoachInsightRequest;
import org.example.subcontracting.coach.dto.CoachInsightResponse;
import org.example.subcontracting.coach.dto.InsightHistoryResponse;
import org.example.subcontracting.coach.entity.CoachInsightHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/coach/insights")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CoachInsightController {

    private final CoachInsightService coachInsightService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/initial")
    public ResponseEntity<CoachInsightResponse> initial(
            @RequestParam Long userId,
            @Valid @RequestBody CoachInsightRequest body) {
        return ResponseEntity.ok(coachInsightService.initialInsight(userId, body));
    }

    @PostMapping("/advanced")
    public ResponseEntity<CoachInsightResponse> advanced(
            @RequestParam Long userId,
            @Valid @RequestBody CoachInsightRequest body) {
        return ResponseEntity.ok(coachInsightService.advancedInsight(userId, body));
    }

    @GetMapping("/history/me")
    public ResponseEntity<Page<InsightHistoryResponse>> myHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable p = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(coachInsightService.myHistory(userId, p).map(this::toHistoryDto));
    }

    @GetMapping("/history/{targetUserId}")
    public ResponseEntity<Page<InsightHistoryResponse>> adminHistory(
            @RequestParam Long adminUserId,
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "adminUserId requis");
        }
        Pageable p = PageRequest.of(page, Math.min(size, 200));
        return ResponseEntity.ok(coachInsightService.adminUserHistory(targetUserId, p).map(this::toHistoryDto));
    }

    private InsightHistoryResponse toHistoryDto(CoachInsightHistory h) {
        JsonNode insight = null;
        if (h.getInsightResultJson() != null && !h.getInsightResultJson().isBlank()) {
            try {
                insight = objectMapper.readTree(h.getInsightResultJson());
            } catch (JsonProcessingException ignored) {
            }
        }
        return InsightHistoryResponse.builder()
                .id(h.getId())
                .userId(h.getUserId())
                .subcontractId(h.getSubcontractId())
                .featureCode(h.getFeatureCode())
                .free(h.getFree())
                .pointsSpent(h.getPointsSpent())
                .insightResult(insight)
                .createdAt(h.getCreatedAt())
                .build();
    }
}
