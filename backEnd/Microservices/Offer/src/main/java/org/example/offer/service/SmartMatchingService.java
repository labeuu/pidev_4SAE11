package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferApplication;
import org.example.offer.entity.OfferStatus;
import org.example.offer.entity.OfferView;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.example.offer.repository.OfferViewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Matching : recommandation des meilleures offres pour un client
 * basée sur l'historique (candidatures) et le comportement (vues).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartMatchingService {

    private static final int CANDIDATE_POOL_SIZE = 300;
    private static final int DEFAULT_LIMIT = 20;
    private static final int VIEW_DAYS_LOOKBACK = 90;

    private final OfferRepository offerRepository;
    private final OfferApplicationRepository applicationRepository;
    private final OfferViewRepository viewRepository;
    private final OfferService offerService;

    /**
     * Recommande les meilleures offres pour un client (historique + comportement).
     *
     * @param clientId ID du client
     * @param limit    nombre max de recommandations (défaut 20)
     * @return liste d'offres triées par pertinence
     */
    @Transactional(readOnly = true)
    public List<OfferResponse> getRecommendedOffersForClient(Long clientId, int limit) {
        if (limit <= 0) limit = DEFAULT_LIMIT;

        // 1) Historique : offres auxquelles le client a déjà candidaté (avec offre chargée)
        List<OfferApplication> applications = applicationRepository.findByClientIdWithOffer(clientId);
        List<Long> appliedOfferIds = applications.stream()
                .map(a -> a.getOffer().getId())
                .distinct()
                .toList();

        // 2) Comportement : vues récentes (offres consultées mais pas forcément candidaté)
        LocalDateTime since = LocalDateTime.now().minusDays(VIEW_DAYS_LOOKBACK);
        List<OfferView> recentViews = viewRepository.findByClientIdAndViewedAtAfter(clientId, since);
        Set<Long> viewedOfferIds = recentViews.stream()
                .map(v -> v.getOffer().getId())
                .collect(Collectors.toSet());

        // 3) Profil préférences (historique + comportement)
        ClientPreferenceProfile profile = buildPreferenceProfile(applications, recentViews);

        // 4) Candidats : offres disponibles, excluant celles déjà candidatées
        List<Offer> candidates;
        if (appliedOfferIds.isEmpty()) {
            candidates = offerRepository.findAvailableOffersForRecommendation(
                    PageRequest.of(0, CANDIDATE_POOL_SIZE));
        } else {
            candidates = offerRepository.findAvailableOffersExcludingIds(
                    appliedOfferIds,
                    PageRequest.of(0, CANDIDATE_POOL_SIZE));
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        // 5) Scoring et tri
        List<ScoredOffer> scored = candidates.stream()
                .map(o -> scoreOffer(o, profile, viewedOfferIds))
                .sorted(Comparator.comparingDouble(ScoredOffer::getScore).reversed())
                .limit(limit)
                .toList();

        List<Offer> topOffers = scored.stream().map(ScoredOffer::getOffer).toList();
        return offerService.mapOffersToResponse(topOffers);
    }

    /**
     * Enregistre une vue d'offre par un client (pour le comportement).
     */
    @Transactional
    public void recordView(Long clientId, Long offerId) {
        if (clientId == null || offerId == null) return;
        offerRepository.findById(offerId).ifPresent(offer -> {
            if (offer.getOfferStatus() != OfferStatus.AVAILABLE) return;
            OfferView view = new OfferView();
            view.setClientId(clientId);
            view.setOffer(offer);
            view.setViewedAt(LocalDateTime.now());
            viewRepository.save(view);
            log.debug("Recorded view: client={}, offer={}", clientId, offerId);
        });
    }

    private ClientPreferenceProfile buildPreferenceProfile(List<OfferApplication> applications, List<OfferView> recentViews) {
        ClientPreferenceProfile profile = new ClientPreferenceProfile();

        // Domaines / catégories / prix issus des candidatures (historique)
        applications.stream()
                .map(OfferApplication::getOffer)
                .filter(Objects::nonNull)
                .forEach(offer -> {
                    if (offer.getDomain() != null && !offer.getDomain().isBlank()) {
                        profile.domainCounts.merge(offer.getDomain(), 1, Integer::sum);
                    }
                    if (offer.getCategory() != null && !offer.getCategory().isBlank()) {
                        profile.categoryCounts.merge(offer.getCategory(), 1, Integer::sum);
                    }
                    if (offer.getPrice() != null) {
                        profile.prices.add(offer.getPrice());
                    }
                    if (offer.getDurationType() != null) {
                        profile.durationTypes.add(offer.getDurationType());
                    }
                });

        // Comportement : domaines/catégories des offres vues récemment
        for (OfferView v : recentViews) {
            Offer o = v.getOffer();
            if (o == null) continue;
            if (o.getDomain() != null && !o.getDomain().isBlank()) {
                profile.domainCounts.merge(o.getDomain(), 1, Integer::sum);
            }
            if (o.getCategory() != null && !o.getCategory().isBlank()) {
                profile.categoryCounts.merge(o.getCategory(), 1, Integer::sum);
            }
        }

        profile.computePriceRange();
        return profile;
    }

    private ScoredOffer scoreOffer(Offer offer, ClientPreferenceProfile profile, Set<Long> viewedOfferIds) {
        double score = 0.0;

        // Domaine préféré (historique + comportement)
        if (offer.getDomain() != null && profile.domainCounts.containsKey(offer.getDomain())) {
            int count = profile.domainCounts.get(offer.getDomain());
            score += 25.0 + Math.min(count * 3, 15);
        }

        // Catégorie préférée
        if (offer.getCategory() != null && profile.categoryCounts.containsKey(offer.getCategory())) {
            score += 15.0;
        }

        // Prix dans la fourchette préférée
        if (offer.getPrice() != null && profile.priceMin != null && profile.priceMax != null) {
            if (offer.getPrice().compareTo(profile.priceMin) >= 0 && offer.getPrice().compareTo(profile.priceMax) <= 0) {
                score += 20.0;
            }
        }

        // Comportement : a consulté cette offre mais n'a pas candidaté (fort signal d'intérêt)
        if (viewedOfferIds.contains(offer.getId())) {
            score += 30.0;
        }

        // Offre en vedette
        if (Boolean.TRUE.equals(offer.getIsFeatured())) {
            score += 8.0;
        }

        // Note (si présente)
        if (offer.getRating() != null && offer.getRating().doubleValue() > 0) {
            score += offer.getRating().doubleValue() * 2;
        }

        // Récence (léger bonus pour les offres récentes)
        if (offer.getCreatedAt() != null) {
            long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(offer.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());
            if (daysSinceCreation <= 30) score += 5;
            else if (daysSinceCreation <= 90) score += 2;
        }

        return new ScoredOffer(offer, score);
    }

    private static class ClientPreferenceProfile {
        Map<String, Integer> domainCounts = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        List<BigDecimal> prices = new ArrayList<>();
        Set<String> durationTypes = new HashSet<>();
        BigDecimal priceMin;
        BigDecimal priceMax;

        void computePriceRange() {
            if (prices.isEmpty()) return;
            priceMin = prices.stream().min(BigDecimal::compareTo).orElse(null);
            priceMax = prices.stream().max(BigDecimal::compareTo).orElse(null);
            // Élargir un peu la fourchette (20 %)
            if (priceMin != null && priceMax != null) {
                BigDecimal range = priceMax.subtract(priceMin);
                BigDecimal margin = range.multiply(BigDecimal.valueOf(0.2));
                priceMin = priceMin.subtract(margin).max(BigDecimal.ZERO);
                priceMax = priceMax.add(margin);
            }
        }
    }

    private static class ScoredOffer {
        private final Offer offer;
        private final double score;

        ScoredOffer(Offer offer, double score) {
            this.offer = offer;
            this.score = score;
        }

        Offer getOffer() { return offer; }
        double getScore() { return score; }
    }
}
