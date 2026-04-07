package org.example.vendor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vendor.client.PortfolioFeignClient;
import org.example.vendor.client.ReviewFeignClient;
import org.example.vendor.client.UserFeignClient;
import org.example.vendor.client.dto.ReviewStatsRemoteDto;
import org.example.vendor.client.dto.SkillRemoteDto;
import org.example.vendor.client.dto.UserNameRemoteDto;
import org.example.vendor.dto.response.MatchProfileResponse;
import org.example.vendor.dto.response.MatchRecommendationResponse;
import org.example.vendor.dto.response.VendorTrustScoreResponse;
import org.example.vendor.entity.FreelancerMatchProfile;
import org.example.vendor.entity.MatchRecommendation;
import org.example.vendor.entity.VendorApproval;
import org.example.vendor.repository.FreelancerMatchProfileRepository;
import org.example.vendor.repository.MatchRecommendationRepository;
import org.example.vendor.repository.VendorApprovalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Talent Matching Engine — intégré au module Vendor.
 *
 * Calcule un profil de matching par freelancer (agrégation multi-services)
 * et génère des recommandations pour projets/offres.
 * Les freelancers avec agrément vendor actif reçoivent un bonus de visibilité (+10 pts).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VendorMatchingService {

    private static final int VENDOR_BOOST_POINTS = 10;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final FreelancerMatchProfileRepository profileRepo;
    private final MatchRecommendationRepository recommendationRepo;
    private final VendorApprovalRepository vendorRepo;
    private final VendorApprovalService vendorApprovalService;
    private final PortfolioFeignClient portfolioClient;
    private final ReviewFeignClient reviewClient;
    private final UserFeignClient userClient;

    // ══════════════════════════════════════════════════════════
    //  Profile computation
    // ══════════════════════════════════════════════════════════

    /**
     * Recalcule le profil de matching d'un freelancer en agrégeant
     * les données de tous les microservices.
     */
    public MatchProfileResponse computeProfile(Long freelancerId) {
        FreelancerMatchProfile profile = profileRepo.findByFreelancerId(freelancerId)
                .orElse(new FreelancerMatchProfile());
        profile.setFreelancerId(freelancerId);

        // User info
        String name = safeUserName(freelancerId);
        profile.setDisplayName(name);

        // Skills (Portfolio)
        List<String> skills = fetchSkills(freelancerId);
        profile.setSkillTags(toJson(skills));
        profile.setPrimaryDomain(skills.isEmpty() ? null : skills.get(0));

        // Reviews
        double[] reviewData = fetchAggregatedReviews(freelancerId);
        profile.setAvgRating(reviewData[0]);
        profile.setReviewCount((long) reviewData[1]);

        // Vendor data
        VendorTrustScoreResponse trust = vendorApprovalService.computeTrustScore(freelancerId);
        profile.setVendorTrustScore(trust.getScore());
        profile.setActiveVendorAgreements((int) trust.getActiveAgreements());
        profile.setVendorBoosted(trust.getActiveAgreements() > 0);
        profile.setCompletedContracts(trust.getTotalAgreements());

        // Compute global score
        int score = computeGlobalScore(profile);
        profile.setGlobalScore(score);
        profile.setLastComputedAt(LocalDateTime.now());

        profile = profileRepo.save(profile);
        log.info("[MATCHING] Profile computed: freelancer={} score={} vendorBoosted={}",
                freelancerId, score, profile.getVendorBoosted());
        return toProfileResponse(profile);
    }

    /**
     * Batch refresh : recalcule tous les profils de freelancers ayant un agrément vendor.
     * Exécuté chaque jour à 3h du matin.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public int refreshAllProfiles() {
        Set<Long> freelancerIds = vendorRepo.findAll().stream()
                .map(VendorApproval::getFreelancerId)
                .collect(Collectors.toSet());
        int count = 0;
        for (Long fId : freelancerIds) {
            try {
                computeProfile(fId);
                count++;
            } catch (Exception e) {
                log.warn("[MATCHING] Failed to refresh profile for freelancer={}: {}", fId, e.getMessage());
            }
        }
        log.info("[MATCHING] Refreshed {} profiles", count);
        return count;
    }

    // ══════════════════════════════════════════════════════════
    //  Read profiles
    // ══════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public MatchProfileResponse getProfile(Long freelancerId) {
        return profileRepo.findByFreelancerId(freelancerId)
                .map(this::toProfileResponse)
                .orElseGet(() -> computeProfile(freelancerId));
    }

    @Transactional(readOnly = true)
    public List<MatchProfileResponse> getTopFreelancers(int limit) {
        return profileRepo.findTopRanked().stream()
                .limit(limit)
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchProfileResponse> getVendorBoostedFreelancers() {
        return profileRepo.findVendorBoosted().stream()
                .map(this::toProfileResponse)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════
    //  Matching : recommend freelancers for a project/offer
    // ══════════════════════════════════════════════════════════

    /**
     * Génère les top N recommandations de freelancers pour un projet ou offre.
     * @param targetType "PROJECT" or "OFFER"
     * @param requiredSkills liste de compétences requises (ex. ["Angular","Java"])
     */
    public List<MatchRecommendationResponse> generateRecommendations(
            String targetType, Long targetId, List<String> requiredSkills, int topN) {

        recommendationRepo.deleteByTargetTypeAndTargetId(targetType, targetId);

        List<FreelancerMatchProfile> all = profileRepo.findTopRanked();
        List<ScoredCandidate> candidates = new ArrayList<>();

        for (FreelancerMatchProfile p : all) {
            List<String> profileSkills = fromJson(p.getSkillTags());
            int skillMatch = computeSkillOverlap(requiredSkills, profileSkills);
            int totalScore = computeMatchScore(p, skillMatch);
            List<String> reasons = buildReasons(p, skillMatch);
            candidates.add(new ScoredCandidate(p, totalScore, reasons));
        }

        candidates.sort(Comparator.comparingInt(ScoredCandidate::score).reversed());

        List<MatchRecommendation> saved = new ArrayList<>();
        int count = 0;
        for (ScoredCandidate c : candidates) {
            if (count >= topN) break;
            MatchRecommendation rec = new MatchRecommendation();
            rec.setTargetType(targetType);
            rec.setTargetId(targetId);
            rec.setFreelancerId(c.profile().getFreelancerId());
            rec.setFreelancerName(c.profile().getDisplayName());
            rec.setMatchScore(c.score());
            rec.setMatchReasons(toJson(c.reasons()));
            rec.setStatus(MatchRecommendation.MatchStatus.SUGGESTED);
            saved.add(recommendationRepo.save(rec));
            count++;
        }

        log.info("[MATCHING] Generated {} recommendations for {}#{}", saved.size(), targetType, targetId);
        return saved.stream().map(this::toRecommendationResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchRecommendationResponse> getRecommendationsForTarget(String targetType, Long targetId) {
        return recommendationRepo.findByTarget(targetType, targetId).stream()
                .map(this::toRecommendationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchRecommendationResponse> getRecommendationsForFreelancer(Long freelancerId) {
        return recommendationRepo.findByFreelancerIdOrderByMatchScoreDesc(freelancerId).stream()
                .map(this::toRecommendationResponse)
                .collect(Collectors.toList());
    }

    public MatchRecommendationResponse updateRecommendationStatus(Long recommendationId, String newStatus) {
        MatchRecommendation rec = recommendationRepo.findById(recommendationId)
                .orElseThrow(() -> new org.example.vendor.exception.ResourceNotFoundException(
                        "Recommandation introuvable : " + recommendationId));
        MatchRecommendation.MatchStatus status = MatchRecommendation.MatchStatus.valueOf(newStatus.toUpperCase());
        rec.setStatus(status);
        if (status == MatchRecommendation.MatchStatus.VIEWED && rec.getViewedAt() == null) {
            rec.setViewedAt(LocalDateTime.now());
        }
        return toRecommendationResponse(recommendationRepo.save(rec));
    }

    // ══════════════════════════════════════════════════════════
    //  Scoring engine
    // ══════════════════════════════════════════════════════════

    private int computeGlobalScore(FreelancerMatchProfile p) {
        int score = 0;

        // Rating : 30 pts max
        double rating = p.getAvgRating() != null ? p.getAvgRating() : 0;
        score += Math.min(30, (int) (rating / 5.0 * 30));

        // Vendor trust : 25 pts max (+ BOOST si agrément actif)
        int trust = p.getVendorTrustScore() != null ? p.getVendorTrustScore() : 0;
        score += Math.min(25, trust / 4);
        if (Boolean.TRUE.equals(p.getVendorBoosted())) {
            score += VENDOR_BOOST_POINTS;
        }

        // Reviews volume : 15 pts max
        long reviews = p.getReviewCount() != null ? p.getReviewCount() : 0;
        score += Math.min(15, (int) (reviews * 1.5));

        // Contracts completed : 15 pts max
        long contracts = p.getCompletedContracts() != null ? p.getCompletedContracts() : 0;
        score += Math.min(15, (int) (contracts * 2));

        // On-time delivery : 15 pts max
        double onTime = p.getOnTimeRate() != null ? p.getOnTimeRate() : 50;
        score += (int) (onTime / 100.0 * 15);

        return Math.min(100, Math.max(0, score));
    }

    private int computeMatchScore(FreelancerMatchProfile p, int skillOverlapPercent) {
        int base = p.getGlobalScore();
        int skillPart = (int) (skillOverlapPercent * 0.40);
        int profilePart = (int) (base * 0.60);
        int total = skillPart + profilePart;
        if (Boolean.TRUE.equals(p.getVendorBoosted())) {
            total = Math.min(100, total + 5);
        }
        return Math.min(100, Math.max(0, total));
    }

    private int computeSkillOverlap(List<String> required, List<String> freelancerSkills) {
        if (required == null || required.isEmpty()) return 50;
        if (freelancerSkills == null || freelancerSkills.isEmpty()) return 0;
        Set<String> reqLower = required.stream().map(String::toLowerCase).collect(Collectors.toSet());
        long matched = freelancerSkills.stream()
                .map(String::toLowerCase)
                .filter(reqLower::contains)
                .count();
        return (int) ((double) matched / required.size() * 100);
    }

    private List<String> buildReasons(FreelancerMatchProfile p, int skillMatch) {
        List<String> reasons = new ArrayList<>();
        reasons.add("skill_match:" + skillMatch + "%");
        if (p.getAvgRating() != null && p.getAvgRating() > 0) {
            reasons.add("rating:" + String.format("%.1f", p.getAvgRating()) + "/5");
        }
        if (Boolean.TRUE.equals(p.getVendorBoosted())) {
            reasons.add("vendor_boost:+15pts");
        }
        if (p.getVendorTrustScore() != null && p.getVendorTrustScore() > 0) {
            reasons.add("trust_score:" + p.getVendorTrustScore());
        }
        if (p.getCompletedContracts() != null && p.getCompletedContracts() > 0) {
            reasons.add("contracts:" + p.getCompletedContracts());
        }
        if (p.getReviewCount() != null && p.getReviewCount() > 0) {
            reasons.add("reviews:" + p.getReviewCount());
        }
        return reasons;
    }

    // ══════════════════════════════════════════════════════════
    //  Data fetchers (Feign calls with fallback)
    // ══════════════════════════════════════════════════════════

    private List<String> fetchSkills(Long freelancerId) {
        try {
            List<SkillRemoteDto> skills = portfolioClient.getSkillsByUser(freelancerId);
            if (skills != null) {
                return skills.stream()
                        .map(SkillRemoteDto::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("[MATCHING] Portfolio unavailable for freelancer={}: {}", freelancerId, e.getMessage());
        }
        return List.of();
    }

    private double[] fetchAggregatedReviews(Long freelancerId) {
        double totalWeighted = 0;
        long totalCount = 0;
        List<VendorApproval> agreements = vendorRepo.findByFreelancerId(freelancerId);
        for (VendorApproval va : agreements) {
            try {
                ReviewStatsRemoteDto stats = reviewClient.getPairStats(va.getOrganizationId(), freelancerId);
                if (stats != null && stats.getTotalCount() > 0) {
                    totalWeighted += stats.getAverageRating() * stats.getTotalCount();
                    totalCount += stats.getTotalCount();
                }
            } catch (Exception e) {
                log.debug("[MATCHING] Review unavailable for org={}: {}", va.getOrganizationId(), e.getMessage());
            }
        }
        double avg = totalCount > 0 ? totalWeighted / totalCount : 0;
        return new double[]{avg, totalCount};
    }

    private String safeUserName(Long userId) {
        try {
            UserNameRemoteDto u = userClient.getUserById(userId);
            if (u != null) {
                String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
                String ln = u.getLastName() != null ? u.getLastName().trim() : "";
                String name = (fn + " " + ln).trim();
                if (!name.isBlank()) return name;
                if (u.getEmail() != null) return u.getEmail();
            }
        } catch (Exception e) {
            log.debug("[MATCHING] User unavailable: {}", e.getMessage());
        }
        return "Freelancer #" + userId;
    }

    // ══════════════════════════════════════════════════════════
    //  Mappers & helpers
    // ══════════════════════════════════════════════════════════

    private MatchProfileResponse toProfileResponse(FreelancerMatchProfile p) {
        return MatchProfileResponse.builder()
                .freelancerId(p.getFreelancerId())
                .displayName(p.getDisplayName())
                .skills(fromJson(p.getSkillTags()))
                .primaryDomain(p.getPrimaryDomain())
                .avgRating(p.getAvgRating())
                .reviewCount(p.getReviewCount())
                .completedContracts(p.getCompletedContracts())
                .onTimeRate(p.getOnTimeRate())
                .vendorTrustScore(p.getVendorTrustScore())
                .activeVendorAgreements(p.getActiveVendorAgreements())
                .vendorBoosted(p.getVendorBoosted())
                .avgResponseTimeHours(p.getAvgResponseTimeHours())
                .globalScore(p.getGlobalScore())
                .globalScoreLabel(scoreLabel(p.getGlobalScore()))
                .lastComputedAt(p.getLastComputedAt())
                .build();
    }

    private MatchRecommendationResponse toRecommendationResponse(MatchRecommendation r) {
        return MatchRecommendationResponse.builder()
                .id(r.getId())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .freelancerId(r.getFreelancerId())
                .freelancerName(r.getFreelancerName())
                .matchScore(r.getMatchScore())
                .matchScoreLabel(scoreLabel(r.getMatchScore()))
                .matchReasons(fromJson(r.getMatchReasons()))
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String scoreLabel(int score) {
        if (score >= 85) return "Excellent match";
        if (score >= 70) return "Très bon match";
        if (score >= 50) return "Bon match";
        if (score >= 30) return "Match partiel";
        return "Match faible";
    }

    private String toJson(Object obj) {
        try { return JSON.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return JSON.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return List.of(); }
    }

    private record ScoredCandidate(FreelancerMatchProfile profile, int score, List<String> reasons) {}
}
