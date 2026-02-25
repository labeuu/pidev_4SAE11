package org.example.offer.service;

import org.example.offer.dto.request.OfferFilterRequest;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.dto.response.OfferStatsResponse;
import org.example.offer.entity.OfferStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface du service métier de gestion des offres.
 * Méthodes métier (CRUD, publication, statut) et avancées (recherche, statistiques).
 */
public interface IOfferService {

    // ========== Métier – CRUD ==========
    OfferResponse createOffer(OfferRequest request);
    OfferResponse getOfferById(Long id);
    List<OfferResponse> getOffersByFreelancer(Long freelancerId);
    OfferResponse updateOffer(Long id, OfferRequest request);
    void deleteOffer(Long id, Long freelancerId);

    // ========== Métier – Publication / statut ==========
    OfferResponse publishOffer(Long id, Long freelancerId);
    OfferResponse changeOfferStatus(Long id, OfferStatus status, Long freelancerId);
    OfferResponse updateScores(Long id, BigDecimal rating, BigDecimal communicationScore);

    // ========== Avancées – Lecture / recherche ==========
    Page<OfferResponse> getActiveOffers(int page, int size);
    List<OfferResponse> getFeaturedOffers();
    Page<OfferResponse> getTopRatedOffers(BigDecimal minRating, int page, int size);
    Page<OfferResponse> searchOffers(OfferFilterRequest filter);
    OfferStatsResponse getFreelancerStats(Long freelancerId);
}
