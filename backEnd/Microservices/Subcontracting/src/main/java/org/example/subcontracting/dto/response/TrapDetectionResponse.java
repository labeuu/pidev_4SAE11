package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrapDetectionResponse {
    private Long subcontractId;
    private String overallSeverity;
    private List<TrapItem> traps;
    private Double confidence;
    private List<AiDecisionReferenceDto> references;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrapItem {
        private String code;
        private String severity;
        private String message;
        private String fixNow;
    }
}

