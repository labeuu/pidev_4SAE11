package org.example.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistiques agrégées pour aider l'admin à décider d'un agrément fournisseur :
 * avis client → freelancer, projets communs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDecisionInsightResponse {

    private Long vendorApprovalId;
    private Long organizationId;
    private Long freelancerId;
    private String clientDisplayName;
    private String freelancerDisplayName;
    private String agreementDomain;
    private String professionalSector;
    private String status;

    private long sharedProjectCount;
    @Builder.Default
    private List<SharedProjectLine> sharedProjects = new ArrayList<>();

    private long reviewCount;
    private double averageRatingFromClient;
    @Builder.Default
    private Map<Integer, Long> ratingDistribution = new HashMap<>();

    @Builder.Default
    private List<ClientToFreelancerReviewLine> reviews = new ArrayList<>();

    /** Messages si un microservice est indisponible (non bloquant). */
    @Builder.Default
    private List<String> dataWarnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SharedProjectLine {
        private Long id;
        private String title;
        private String status;
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientToFreelancerReviewLine {
        private Long id;
        private Long projectId;
        private Integer rating;
        private String comment;
        private String createdAt;
    }
}
