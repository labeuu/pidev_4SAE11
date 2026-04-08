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
public class MatchProfileResponse {

    private Long freelancerId;
    private String displayName;
    private List<String> skills;
    private String primaryDomain;
    private Double avgRating;
    private Long reviewCount;
    private Long completedContracts;
    private Double onTimeRate;
    private Integer vendorTrustScore;
    private Integer activeVendorAgreements;
    private Boolean vendorBoosted;
    private Double avgResponseTimeHours;
    private Integer globalScore;
    private String globalScoreLabel;
    private LocalDateTime lastComputedAt;
}
