package org.example.offer.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.client.ProjectStatusClient;
import org.example.offer.dto.external.ProjectStatusDTO;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.example.offer.exception.BadRequestException;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OfferService {

    private final OfferRepository offerRepository;
    private final ProjectStatusClient projectStatusClient;
    private final ModelMapper modelMapper;

    /**
     * CREATE - Créer une nouvelle offre
     */
    @CircuitBreaker(name = "projectService", fallbackMethod = "createOfferFallback")
    public OfferResponse createOffer(OfferRequest request) {
        log.info("Creating new offer for freelancer: {}", request.getFreelancerId());

        // Valider le ProjectStatus si fourni
        if (request.getProjectStatusId() != null) {
            ProjectStatusDTO projectStatus = projectStatusClient.getProjectStatusById(request.getProjectStatusId());
            if (projectStatus == null) {
                throw new BadRequestException("Invalid project status ID: " + request.getProjectStatusId());
            }
        }

        Offer offer = modelMapper.map(request, Offer.class);
        offer.setOfferStatus(OfferStatus.DRAFT);

        Offer savedOffer = offerRepository.save(offer);
        log.info("Offer created successfully with ID: {}", savedOffer.getId());

        return mapToResponse(savedOffer);
    }

    /**
     * Fallback si le service PROJECT est down
     */
    private OfferResponse createOfferFallback(OfferRequest request, Exception ex) {
        log.error("Project service is down, creating offer without project status validation", ex);

        Offer offer = modelMapper.map(request, Offer.class);
        offer.setOfferStatus(OfferStatus.DRAFT);

        Offer savedOffer = offerRepository.save(offer);
        return mapToResponse(savedOffer);
    }

    /**
     * READ - Récupérer une offre par ID
     */
    public OfferResponse getOfferById(Long id) {
        log.info("Fetching offer with ID: {}", id);

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));

        offer.incrementViews();
        offerRepository.save(offer);

        return mapToResponse(offer);
    }

    /**
     * READ - Récupérer toutes les offres d'un freelancer
     */
    public List<OfferResponse> getOffersByFreelancer(Long freelancerId) {
        log.info("Fetching offers for freelancer: {}", freelancerId);

        return offerRepository.findByFreelancerId(freelancerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * UPDATE - Mettre à jour une offre
     */
    @CircuitBreaker(name = "projectService", fallbackMethod = "updateOfferFallback")
    public OfferResponse updateOffer(Long id, OfferRequest request) {
        log.info("Updating offer with ID: {}", id);

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));

        if (!offer.getFreelancerId().equals(request.getFreelancerId())) {
            throw new BadRequestException("You are not authorized to update this offer");
        }

        if (offer.getOfferStatus() == OfferStatus.ACCEPTED) {
            throw new BadRequestException("Cannot update an accepted offer");
        }

        // Valider le nouveau ProjectStatus si modifié
        if (request.getProjectStatusId() != null) {
            ProjectStatusDTO projectStatus = projectStatusClient.getProjectStatusById(request.getProjectStatusId());
            if (projectStatus == null) {
                throw new BadRequestException("Invalid project status ID: " + request.getProjectStatusId());
            }
        }

        // Mise à jour des champs
        offer.setTitle(request.getTitle());
        offer.setDomain(request.getDomain());
        offer.setDescription(request.getDescription());
        offer.setPrice(request.getPrice());
        offer.setDurationType(request.getDurationType());
        offer.setDeadline(request.getDeadline());
        offer.setCategory(request.getCategory());
        offer.setTags(request.getTags());
        offer.setImageUrl(request.getImageUrl());
        offer.setProjectStatusId(request.getProjectStatusId());

        if (request.getRating() != null) {
            offer.setRating(request.getRating());
        }
        if (request.getCommunicationScore() != null) {
            offer.setCommunicationScore(request.getCommunicationScore());
        }
        if (request.getIsFeatured() != null) {
            offer.setIsFeatured(request.getIsFeatured());
        }

        Offer updatedOffer = offerRepository.save(offer);
        log.info("Offer updated successfully: {}", id);

        return mapToResponse(updatedOffer);
    }

    /**
     * Fallback pour updateOffer
     */
    private OfferResponse updateOfferFallback(Long id, OfferRequest request, Exception ex) {
        log.error("Project service is down, updating offer without project status validation", ex);

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        offer.setTitle(request.getTitle());
        offer.setDomain(request.getDomain());
        offer.setDescription(request.getDescription());
        offer.setPrice(request.getPrice());
        // ... autres champs

        return mapToResponse(offerRepository.save(offer));
    }

    /**
     * DELETE - Supprimer une offre
     */
    public void deleteOffer(Long id, Long freelancerId) {
        log.info("Deleting offer with ID: {}", id);

        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));

        if (!offer.getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("You are not authorized to delete this offer");
        }

        if (offer.getOfferStatus() == OfferStatus.ACCEPTED ||
                offer.getOfferStatus() == OfferStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot delete an accepted or in-progress offer");
        }

        offerRepository.delete(offer);
        log.info("Offer deleted successfully: {}", id);
    }

    /**
     * Mapper Offer → OfferResponse avec enrichissement du ProjectStatus
     */
    @CircuitBreaker(name = "projectService", fallbackMethod = "mapToResponseFallback")
    private OfferResponse mapToResponse(Offer offer) {
        OfferResponse response = modelMapper.map(offer, OfferResponse.class);
        response.setApplicationsCount(offer.getApplicationsCount());
        response.setPendingApplicationsCount(offer.getPendingApplicationsCount());
        response.setCanReceiveApplications(offer.canReceiveApplications());
        response.setIsValid(offer.isValid());

        // ✅ Enrichir avec le ProjectStatus via Feign
        if (offer.getProjectStatusId() != null) {
            try {
                ProjectStatusDTO projectStatus = projectStatusClient.getProjectStatusById(offer.getProjectStatusId());
                response.setProjectStatus(projectStatus);
            } catch (Exception e) {
                log.warn("Failed to fetch project status for offer {}: {}", offer.getId(), e.getMessage());
            }
        }

        return response;
    }

    /**
     * Fallback pour mapToResponse
     */
    private OfferResponse mapToResponseFallback(Offer offer, Exception ex) {
        log.error("Failed to enrich offer with project status", ex);

        OfferResponse response = modelMapper.map(offer, OfferResponse.class);
        response.setApplicationsCount(offer.getApplicationsCount());
        response.setPendingApplicationsCount(offer.getPendingApplicationsCount());
        response.setCanReceiveApplications(offer.canReceiveApplications());
        response.setIsValid(offer.isValid());
        // ProjectStatus sera null

        return response;
    }
}