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
public class FreelancerMatchDecisionResponse {
    private Long subcontractId;
    private String summary;
    private List<CandidateDecision> candidates;
    private Double confidence;
    private List<AiDecisionReferenceDto> references;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateDecision {
        private Long freelancerId;
        private String fullName;
        private Integer matchScore;
        private String recommendation;
        private String explanation;
        private Integer trustScore;
        private Long previousCollaborations;
        private Integer riskPenalty;
        private Double confidence;
    }
}

