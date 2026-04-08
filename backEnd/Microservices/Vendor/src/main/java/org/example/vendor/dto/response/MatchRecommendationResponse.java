package org.example.vendor.dto.response;

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
public class MatchRecommendationResponse {

    private Long id;
    private String targetType;
    private Long targetId;
    private Long freelancerId;
    private String freelancerName;
    private Integer matchScore;
    private String matchScoreLabel;
    private List<String> matchReasons;
    private String status;
    private LocalDateTime createdAt;
}
