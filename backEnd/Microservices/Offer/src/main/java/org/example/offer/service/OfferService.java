package org.example.offer.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.dto.request.OfferFilterRequest;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.dto.response.OfferStatsResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.Offer;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferApplicationRepository applicationRepository;
    private final ModelMapper modelMapper;

    /**
     * CREATE - Créer une nouvelle offre
     */
    public OfferResponse createOffer(OfferRequest request) {
        log.info("Creating new offer for freelancer: {}", request.getFreelancerId());

        Offer offer = modelMapper.map(request, Offer.class);
        offer.setOfferStatus(OfferStatus.DRAFT);

        Offer savedOffer = offerRepository.save(offer);
        log.info("Offer created successfully with ID: {}", savedOffer.getId());

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
     * READ - Offres actives (paginated)
     */
    public Page<OfferResponse> getActiveOffers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return offerRepository.findActiveOffers(pageable).map(this::mapToResponse);
    }

    /**
     * READ - Offres featured
     */
    public List<OfferResponse> getFeaturedOffers() {
        return offerRepository.findFeaturedOffers()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * READ - Offres les mieux notées
     */
    public Page<OfferResponse> getTopRatedOffers(BigDecimal minRating, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return offerRepository.findTopRatedOffers(minRating, pageable).map(this::mapToResponse);
    }

    /**
     * READ - Recherche avec filtres
     */
    public Page<OfferResponse> searchOffers(OfferFilterRequest filter) {
        Specification<Offer> spec = buildSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.fromString(
                filter.getSortDirection() != null ? filter.getSortDirection() : "DESC"),
                filter.getSortBy() != null ? filter.getSortBy() : "createdAt");
        Pageable pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 10,
                sort);
        return offerRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    /**
     * READ - Statistiques freelancer
     */
    public OfferStatsResponse getFreelancerStats(Long freelancerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime oneWeekAgo = now.minusWeeks(1);

        Long totalOffers = offerRepository.countByFreelancerId(freelancerId);
        Long activeOffers = offerRepository.countByFreelancerIdAndStatus(freelancerId, OfferStatus.AVAILABLE);
        Long draftOffers = offerRepository.countByFreelancerIdAndStatus(freelancerId, OfferStatus.DRAFT);
        Long acceptedOffers = offerRepository.countByFreelancerIdAndStatus(freelancerId, OfferStatus.ACCEPTED);
        Long expiredOffers = offerRepository.countByFreelancerIdAndStatus(freelancerId, OfferStatus.EXPIRED);
        Long totalApplications = applicationRepository.countByOffer_FreelancerId(freelancerId);
        Long pendingApplications = (long) applicationRepository.findByFreelancerIdAndStatus(freelancerId, ApplicationStatus.PENDING).size();
        BigDecimal averageRating = offerRepository.calculateAverageRating(freelancerId);
        BigDecimal totalRevenue = offerRepository.calculateTotalRevenue(freelancerId);
        Long totalViews = offerRepository.sumViewsByFreelancerId(freelancerId);
        Long offersThisMonth = offerRepository.countByFreelancerIdAndCreatedAtBetween(freelancerId, startOfMonth, now);
        Long offersThisWeek = offerRepository.countByFreelancerIdAndCreatedAtBetween(freelancerId, oneWeekAgo, now);

        OfferStatsResponse stats = new OfferStatsResponse();
        stats.setTotalOffers(totalOffers);
        stats.setActiveOffers(activeOffers);
        stats.setDraftOffers(draftOffers);
        stats.setAcceptedOffers(acceptedOffers);
        stats.setExpiredOffers(expiredOffers);
        stats.setTotalApplications(totalApplications);
        stats.setPendingApplications(pendingApplications);
        stats.setAverageRating(averageRating != null ? averageRating : BigDecimal.ZERO);
        stats.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.setTotalViews(totalViews != null ? totalViews.intValue() : 0);
        stats.setOffersThisMonth(offersThisMonth);
        stats.setOffersThisWeek(offersThisWeek);
        return stats;
    }

    /**
     * UPDATE - Publier une offre
     */
    public OfferResponse publishOffer(Long id, Long freelancerId) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
        if (!offer.getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("You are not authorized to publish this offer");
        }
        offer.publish();
        return mapToResponse(offerRepository.save(offer));
    }

    /**
     * UPDATE - Changer le statut d'une offre
     */
    public OfferResponse changeOfferStatus(Long id, OfferStatus status, Long freelancerId) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
        if (!offer.getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("You are not authorized to change this offer status");
        }
        offer.setOfferStatus(status);
        if (status == OfferStatus.ACCEPTED) {
            offer.accept();
        } else if (status == OfferStatus.EXPIRED) {
            offer.expire();
        } else if (status == OfferStatus.CLOSED) {
            offer.deactivate();
        }
        return mapToResponse(offerRepository.save(offer));
    }

    /**
     * UPDATE - Mettre à jour les scores (rating, communicationScore)
     */
    public OfferResponse updateScores(Long id, BigDecimal rating, BigDecimal communicationScore) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
        if (rating != null) {
            offer.setRating(rating);
        }
        if (communicationScore != null) {
            offer.setCommunicationScore(communicationScore);
        }
        return mapToResponse(offerRepository.save(offer));
    }

    /**
     * UPDATE - Mettre à jour une offre
     */
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
     * Construction d'une Specification à partir des filtres
     */
    private Specification<Offer> buildSpecification(OfferFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String pattern = "%" + filter.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("tags")), pattern)
                ));
            }
            if (filter.getDomain() != null && !filter.getDomain().isBlank()) {
                predicates.add(cb.equal(root.get("domain"), filter.getDomain()));
            }
            if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }
            if (filter.getOfferStatus() != null) {
                predicates.add(cb.equal(root.get("offerStatus"), filter.getOfferStatus()));
            }
            if (filter.getProjectStatusId() != null) {
                predicates.add(cb.equal(root.get("projectStatusId"), filter.getProjectStatusId()));
            }
            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }
            if (filter.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filter.getMinRating()));
            }
            if (filter.getDurationType() != null && !filter.getDurationType().isBlank()) {
                predicates.add(cb.equal(root.get("durationType"), filter.getDurationType()));
            }
            if (filter.getIsFeatured() != null) {
                predicates.add(cb.equal(root.get("isFeatured"), filter.getIsFeatured()));
            }
            if (filter.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
            }
            if (filter.getFreelancerId() != null) {
                predicates.add(cb.equal(root.get("freelancerId"), filter.getFreelancerId()));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Mapper Offer → OfferResponse
     */
    private OfferResponse mapToResponse(Offer offer) {
        OfferResponse response = modelMapper.map(offer, OfferResponse.class);
        response.setApplicationsCount(offer.getApplicationsCount());
        response.setPendingApplicationsCount(offer.getPendingApplicationsCount());
        response.setCanReceiveApplications(offer.canReceiveApplications());
        response.setIsValid(offer.isValid());
        response.setProjectStatusId(offer.getProjectStatusId());
        return response;
    }
}