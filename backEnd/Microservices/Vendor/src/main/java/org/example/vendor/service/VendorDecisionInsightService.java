package org.example.vendor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vendor.client.ProjectFeignClient;
import org.example.vendor.client.ReviewFeignClient;
import org.example.vendor.client.UserFeignClient;
import org.example.vendor.client.dto.JointProjectItemRemoteDto;
import org.example.vendor.client.dto.JointProjectsRemoteDto;
import org.example.vendor.client.dto.ReviewRemoteDto;
import org.example.vendor.client.dto.ReviewStatsRemoteDto;
import org.example.vendor.client.dto.UserNameRemoteDto;
import org.example.vendor.dto.response.VendorDecisionInsightResponse;
import org.example.vendor.entity.VendorApproval;
import org.example.vendor.exception.ResourceNotFoundException;
import org.example.vendor.repository.VendorApprovalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorDecisionInsightService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final VendorApprovalRepository repository;
    private final ReviewFeignClient reviewFeignClient;
    private final ProjectFeignClient projectFeignClient;
    private final UserFeignClient userFeignClient;
    private final VendorDecisionPdfService pdfService;

    @Transactional(readOnly = true)
    public VendorDecisionInsightResponse buildInsight(Long vendorApprovalId) {
        VendorApproval va = repository.findById(vendorApprovalId)
                .orElseThrow(() -> new ResourceNotFoundException("Agrément introuvable : " + vendorApprovalId));

        Long clientId = va.getOrganizationId();
        Long freelancerId = va.getFreelancerId();

        String clientName = safeUserLabel(clientId, "Client");
        String freelancerName = safeUserLabel(freelancerId, "Freelancer");

        List<String> warnings = new ArrayList<>();

        ReviewStatsRemoteDto stats = null;
        List<ReviewRemoteDto> reviewList = List.of();
        try {
            stats = reviewFeignClient.getPairStats(clientId, freelancerId);
            reviewList = reviewFeignClient.getPairReviews(clientId, freelancerId);
            if (reviewList == null) {
                reviewList = List.of();
            }
        } catch (Exception e) {
            log.warn("[VENDOR-INSIGHT] Review service: {}", e.getMessage());
            warnings.add("Avis (Review) : données partielles ou indisponibles — " + e.getMessage());
        }

        JointProjectsRemoteDto joint = null;
        try {
            joint = projectFeignClient.getJointProjects(clientId, freelancerId);
        } catch (Exception e) {
            log.warn("[VENDOR-INSIGHT] Project service: {}", e.getMessage());
            warnings.add("Projets : données partielles ou indisponibles — " + e.getMessage());
        }

        Map<Integer, Long> dist = new HashMap<>();
        long reviewCount = 0;
        double avg = 0;
        if (stats != null) {
            reviewCount = stats.getTotalCount();
            avg = stats.getAverageRating();
            if (stats.getCountByRating() != null) {
                dist.putAll(stats.getCountByRating());
            }
        }

        List<VendorDecisionInsightResponse.SharedProjectLine> projects = new ArrayList<>();
        long projectCount = 0;
        if (joint != null && joint.getProjects() != null) {
            projectCount = joint.getSharedProjectCount();
            projects = joint.getProjects().stream()
                    .map(this::mapProject)
                    .collect(Collectors.toList());
        }

        List<VendorDecisionInsightResponse.ClientToFreelancerReviewLine> lines = reviewList.stream()
                .map(this::mapReview)
                .collect(Collectors.toList());

        return VendorDecisionInsightResponse.builder()
                .vendorApprovalId(va.getId())
                .organizationId(clientId)
                .freelancerId(freelancerId)
                .clientDisplayName(clientName)
                .freelancerDisplayName(freelancerName)
                .agreementDomain(va.getDomain())
                .professionalSector(va.getProfessionalSector())
                .status(va.getStatus() != null ? va.getStatus().name() : null)
                .sharedProjectCount(projectCount)
                .sharedProjects(projects)
                .reviewCount(reviewCount)
                .averageRatingFromClient(avg)
                .ratingDistribution(dist)
                .reviews(lines)
                .dataWarnings(warnings)
                .build();
    }

    public byte[] buildPdf(Long vendorApprovalId) {
        VendorDecisionInsightResponse insight = buildInsight(vendorApprovalId);
        return pdfService.generate(insight);
    }

    private VendorDecisionInsightResponse.SharedProjectLine mapProject(JointProjectItemRemoteDto p) {
        VendorDecisionInsightResponse.SharedProjectLine line = new VendorDecisionInsightResponse.SharedProjectLine();
        line.setId(p.getId());
        line.setTitle(p.getTitle());
        line.setStatus(p.getStatus());
        line.setCategory(p.getCategory());
        return line;
    }

    private VendorDecisionInsightResponse.ClientToFreelancerReviewLine mapReview(ReviewRemoteDto r) {
        String when = r.getCreatedAt() != null ? ISO.format(r.getCreatedAt()) : "";
        return new VendorDecisionInsightResponse.ClientToFreelancerReviewLine(
                r.getId(),
                r.getProjectId(),
                r.getRating(),
                r.getComment(),
                when
        );
    }

    private String safeUserLabel(Long userId, String fallbackPrefix) {
        try {
            UserNameRemoteDto u = userFeignClient.getUserById(userId);
            if (u == null) {
                return fallbackPrefix + " #" + userId;
            }
            String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
            String ln = u.getLastName() != null ? u.getLastName().trim() : "";
            String name = (fn + " " + ln).trim();
            if (!name.isBlank()) {
                return name;
            }
            if (u.getEmail() != null && !u.getEmail().isBlank()) {
                return u.getEmail();
            }
        } catch (Exception e) {
            log.debug("[VENDOR-INSIGHT] User {} : {}", userId, e.getMessage());
        }
        return fallbackPrefix + " #" + userId;
    }
}
