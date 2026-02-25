package org.example.offer.service;

import org.example.offer.dto.request.OfferApplicationRequest;
import org.example.offer.dto.response.OfferApplicationResponse;
import org.example.offer.entity.ApplicationStatus;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Interface du service métier de gestion des candidatures aux offres.
 * Méthodes métier (CRUD, accepter, rejeter, shortlist) et avancées (requêtes, comptages).
 */
public interface IOfferApplicationService {

    // ========== Métier – CRUD ==========
    OfferApplicationResponse applyToOffer(OfferApplicationRequest request);
    OfferApplicationResponse getApplicationById(Long id);
    OfferApplicationResponse updateApplication(Long id, OfferApplicationRequest request);
    void deleteApplication(Long id, Long clientId);

    // ========== Métier – Workflow ==========
    OfferApplicationResponse acceptApplication(Long id, Long freelancerId);
    OfferApplicationResponse rejectApplication(Long id, Long freelancerId, String reason);
    OfferApplicationResponse shortlistApplication(Long id, Long freelancerId);
    OfferApplicationResponse withdrawApplication(Long id, Long clientId);
    OfferApplicationResponse markAsRead(Long id, Long freelancerId);

    // ========== Avancées – Lecture ==========
    Page<OfferApplicationResponse> getApplicationsByOffer(Long offerId, int page, int size);
    Page<OfferApplicationResponse> getApplicationsByClient(Long clientId, int page, int size);
    List<OfferApplicationResponse> getPendingApplications();
    List<OfferApplicationResponse> getUnreadApplicationsByFreelancer(Long freelancerId);
    List<OfferApplicationResponse> getApplicationsByOfferAndStatus(Long offerId, ApplicationStatus status);
    Long countPendingApplications(Long offerId);
    List<OfferApplicationResponse> getRecentApplications();
}
