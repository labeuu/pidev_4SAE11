package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.client.ContractClient;
import org.example.offer.client.ContractCreateRequest;
import org.example.offer.dto.request.OfferApplicationRequest;
import org.example.offer.dto.response.OfferApplicationResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferApplication;
import org.example.offer.entity.OfferStatus;
import org.example.offer.exception.BadRequestException;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OfferApplicationService implements IOfferApplicationService {

    private final OfferApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;
    private final ModelMapper modelMapper;
    private final ContractClient contractClient;

    /**
     * CREATE - Postuler à une offre
     */
    public OfferApplicationResponse applyToOffer(OfferApplicationRequest request) {
        log.info("Client {} applying to offer {}", request.getClientId(), request.getOfferId());

        // Vérifier que l'offre existe
        Offer offer = offerRepository.findById(request.getOfferId())
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + request.getOfferId()));

        // Validation métier
        validateApplication(offer, request.getClientId());

        // Créer la candidature (manual mapping to avoid ModelMapper confusing setId with getOfferId/getClientId)
        OfferApplication application = new OfferApplication();
        application.setOffer(offer);
        application.setClientId(request.getClientId());
        application.setMessage(request.getMessage());
        application.setProposedBudget(request.getProposedBudget());
        application.setPortfolioUrl(request.getPortfolioUrl());
        application.setAttachmentUrl(request.getAttachmentUrl());
        application.setEstimatedDuration(request.getEstimatedDuration());
        application.setStatus(ApplicationStatus.PENDING);

        OfferApplication savedApplication = applicationRepository.save(application);
        log.info("Application created successfully with ID: {}", savedApplication.getId());

        return mapToResponse(savedApplication);
    }

    /**
     * READ - Récupérer une candidature par ID
     */
    public OfferApplicationResponse getApplicationById(Long id) {
        log.info("Fetching application with ID: {}", id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        return mapToResponse(application);
    }

    /**
     * READ - Récupérer toutes les candidatures d'une offre
     */
    public Page<OfferApplicationResponse> getApplicationsByOffer(Long offerId, int page, int size) {
        log.info("Fetching applications for offer: {}", offerId);

        // Vérifier que l'offre existe
        if (!offerRepository.existsById(offerId)) {
            throw new ResourceNotFoundException("Offer not found with id: " + offerId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        return applicationRepository.findByOfferId(offerId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * READ - Récupérer toutes les candidatures d'un client
     */
    public Page<OfferApplicationResponse> getApplicationsByClient(Long clientId, int page, int size) {
        log.info("Fetching applications for client: {}", clientId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        return applicationRepository.findByClientId(clientId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * READ - Récupérer les candidatures en attente
     */
    public List<OfferApplicationResponse> getPendingApplications() {
        log.info("Fetching all pending applications");

        return applicationRepository.findByStatus(ApplicationStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * READ - Récupérer les candidatures non lues d'un freelancer
     */
    public List<OfferApplicationResponse> getUnreadApplicationsByFreelancer(Long freelancerId) {
        log.info("Fetching unread applications for freelancer: {}", freelancerId);

        return applicationRepository.findUnreadApplicationsByFreelancer(freelancerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * READ - Récupérer les candidatures d'une offre par statut
     */
    public List<OfferApplicationResponse> getApplicationsByOfferAndStatus(Long offerId, ApplicationStatus status) {
        log.info("Fetching applications for offer {} with status {}", offerId, status);

        return applicationRepository.findByOfferIdAndStatus(offerId, status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * READ - Compter les candidatures en attente pour une offre
     */
    public Long countPendingApplications(Long offerId) {
        log.info("Counting pending applications for offer: {}", offerId);
        return applicationRepository.countPendingApplications(offerId);
    }

    /**
     * UPDATE - Accepter une candidature
     */
    public OfferApplicationResponse acceptApplication(Long id, Long freelancerId) {
        log.info("Accepting application with ID: {}", id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validation : seul le propriétaire de l'offre peut accepter
        if (!application.getOffer().getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("You are not authorized to accept this application");
        }

        // Validation : la candidature doit être en attente
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be accepted");
        }

        // Accepter la candidature
        application.accept();

        Offer offer = application.getOffer();
        if (offer.getOfferStatus() == OfferStatus.AVAILABLE) {
            offer.setOfferStatus(OfferStatus.IN_PROGRESS);
            offerRepository.save(offer);
        }

        OfferApplication updatedApplication = applicationRepository.save(application);

        // Création automatique d'un Contract via le microservice Contract (workflow type Fiverr)
        ContractCreateRequest contractRequest = ContractCreateRequest.builder()
                .clientId(application.getClientId())
                .freelancerId(offer.getFreelancerId())
                .offerApplicationId(application.getId())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .terms("Contract from offer: " + offer.getTitle())
                .amount(application.getProposedBudget() != null ? application.getProposedBudget() : offer.getPrice() != null ? offer.getPrice() : BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .endDate(offer.getDeadline())
                .status("DRAFT")
                .build();
        contractClient.createContractFromAcceptedApplication(contractRequest);

        log.info("Application accepted successfully: {}, contract created", id);
        return mapToResponse(updatedApplication);
    }

    /**
     * UPDATE - Rejeter une candidature
     */
    public OfferApplicationResponse rejectApplication(Long id, Long freelancerId, String reason) {
        log.info("Rejecting application with ID: {}", id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validation : seul le propriétaire de l'offre peut rejeter
        if (!application.getOffer().getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("You are not authorized to reject this application");
        }

        // Validation : la candidature doit être en attente
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be rejected");
        }

        // Rejeter la candidature
        application.reject(reason);

        OfferApplication updatedApplication = applicationRepository.save(application);
        log.info("Application rejected successfully: {}", id);

        return mapToResponse(updatedApplication);
    }

    /**
     * UPDATE - Mettre en liste courte (shortlist)
     */
    public OfferApplicationResponse shortlistApplication(Long id, Long freelancerId) {
        log.info("Shortlisting application with ID: {}", id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validation : seul le propriétaire de l'offre peut shortlister
        if (!application.getOffer().getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("You are not authorized to shortlist this application");
        }

        // Validation : la candidature doit être en attente
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be shortlisted");
        }

        application.setStatus(ApplicationStatus.SHORTLISTED);

        OfferApplication updatedApplication = applicationRepository.save(application);
        log.info("Application shortlisted successfully: {}", id);

        return mapToResponse(updatedApplication);
    }

    /**
     * UPDATE - Marquer comme lu
     */
    public OfferApplicationResponse markAsRead(Long id, Long freelancerId) {
        log.info("Marking application {} as read", id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validation : seul le propriétaire de l'offre peut marquer comme lu
        if (!application.getOffer().getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("You are not authorized to mark this application as read");
        }

        application.markAsRead();

        OfferApplication updatedApplication = applicationRepository.save(application);
        log.info("Application marked as read: {}", id);

        return mapToResponse(updatedApplication);
    }

    /**
     * UPDATE - Retirer une candidature (par le client)
     */
    public OfferApplicationResponse withdrawApplication(Long id, Long clientId) {
        log.info("Client {} withdrawing application {}", clientId, id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validation : seul le client propriétaire peut retirer
        if (!application.getClientId().equals(clientId)) {
            throw new BadRequestException("You are not authorized to withdraw this application");
        }

        // Validation : on peut seulement retirer une candidature en attente ou shortlistée
        if (application.getStatus() != ApplicationStatus.PENDING &&
                application.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw new BadRequestException("Only pending or shortlisted applications can be withdrawn");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);

        OfferApplication updatedApplication = applicationRepository.save(application);
        log.info("Application withdrawn successfully: {}", id);

        return mapToResponse(updatedApplication);
    }

    /**
     * UPDATE - Modifier une candidature
     */
    public OfferApplicationResponse updateApplication(Long id, OfferApplicationRequest request) {
        log.info("Updating application with ID: {}", id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validation : seul le client propriétaire peut modifier
        if (!application.getClientId().equals(request.getClientId())) {
            throw new BadRequestException("You are not authorized to update this application");
        }

        // Validation : on peut seulement modifier une candidature en attente
        if (!application.canBeModified()) {
            throw new BadRequestException("Only pending applications can be modified");
        }

        // Mise à jour des champs
        application.setMessage(request.getMessage());
        application.setProposedBudget(request.getProposedBudget());
        application.setPortfolioUrl(request.getPortfolioUrl());
        application.setAttachmentUrl(request.getAttachmentUrl());
        application.setEstimatedDuration(request.getEstimatedDuration());

        OfferApplication updatedApplication = applicationRepository.save(application);
        log.info("Application updated successfully: {}", id);

        return mapToResponse(updatedApplication);
    }

    /**
     * DELETE - Supprimer une candidature
     */
    public void deleteApplication(Long id, Long clientId) {
        log.info("Deleting application with ID: {}", id);

        OfferApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Validation : seul le client propriétaire peut supprimer
        if (!application.getClientId().equals(clientId)) {
            throw new BadRequestException("You are not authorized to delete this application");
        }

        // Validation : on peut seulement supprimer une candidature en attente ou rejetée
        if (application.getStatus() == ApplicationStatus.ACCEPTED) {
            throw new BadRequestException("Cannot delete an accepted application");
        }

        applicationRepository.delete(application);
        log.info("Application deleted successfully: {}", id);
    }

    /**
     * Récupérer les candidatures récentes (dernières 24h)
     */
    public List<OfferApplicationResponse> getRecentApplications() {
        log.info("Fetching recent applications (last 24 hours)");

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        return applicationRepository.findRecentApplications(yesterday)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== Méthodes privées de validation ==========

    private void validateApplication(Offer offer, Long clientId) {
        // L'offre doit accepter des candidatures
        if (!offer.canReceiveApplications()) {
            throw new BadRequestException("This offer is not accepting applications");
        }

        // Le client ne peut pas postuler à sa propre offre
        if (offer.getFreelancerId().equals(clientId)) {
            throw new BadRequestException("You cannot apply to your own offer");
        }

        // Vérifier si le client a déjà postulé
        if (applicationRepository.existsByOfferIdAndClientId(offer.getId(), clientId)) {
            throw new BadRequestException("You have already applied to this offer");
        }
    }

    private OfferApplicationResponse mapToResponse(OfferApplication application) {
        OfferApplicationResponse response = modelMapper.map(application, OfferApplicationResponse.class);
        response.setOfferId(application.getOffer().getId());
        response.setOfferTitle(application.getOffer().getTitle());
        response.setCanBeModified(application.canBeModified());
        return response;
    }
}