package org.example.offer.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.dto.request.OfferFilterRequest;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.response.AcceptanceRateResponse;
import org.example.offer.dto.response.MonthlyEvolutionResponse;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.dto.response.OfferStatsResponse;
import org.example.offer.dto.response.OffersByStatusResponse;
import org.example.offer.dto.response.TranslateOfferResponse;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OfferService implements IOfferService {

    private final OfferRepository offerRepository;
    private final OfferApplicationRepository applicationRepository;
    private final ModelMapper modelMapper;
    private final TranslationService translationService;

    /**
     * CREATE - Créer une nouvelle offre
     */
    public OfferResponse createOffer(OfferRequest request) {
        log.info("Creating new offer for freelancer: {}", request.getFreelancerId());

        Offer offer = modelMapper.map(request, Offer.class);
        offer.setOfferStatus(OfferStatus.DRAFT);
        // Ensure non-null defaults so persist does not fail (ModelMapper may set null from request)
        if (offer.getIsFeatured() == null) {
            offer.setIsFeatured(false);
        }
        if (offer.getIsActive() == null) {
            offer.setIsActive(true);
        }
        if (offer.getViewsCount() == null) {
            offer.setViewsCount(0);
        }
        if (offer.getRating() == null) {
            offer.setRating(BigDecimal.ZERO);
        }
        if (offer.getCommunicationScore() == null) {
            offer.setCommunicationScore(BigDecimal.ZERO);
        }
        if (offer.getApplications() == null) {
            offer.setApplications(new ArrayList<OfferApplication>());
        }

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
     * Statistiques : nombre d'offres par statut (agrégation backend).
     */
    public OffersByStatusResponse getOffersCountByStatus(Long freelancerId) {
        Map<String, Long> countByStatus = new LinkedHashMap<>();
        for (OfferStatus status : OfferStatus.values()) {
            Long count = offerRepository.countByFreelancerIdAndStatus(freelancerId, status);
            countByStatus.put(status.name(), count != null ? count : 0L);
        }
        return new OffersByStatusResponse(countByStatus);
    }

    /**
     * Taux d'acceptation des candidatures (total vs acceptées).
     */
    public AcceptanceRateResponse getAcceptanceRate(Long freelancerId) {
        Long total = applicationRepository.countByOffer_FreelancerId(freelancerId);
        Long accepted = applicationRepository.countAcceptedByFreelancerId(freelancerId);
        if (total == null || total == 0) {
            return new AcceptanceRateResponse(0L, 0L, BigDecimal.ZERO);
        }
        BigDecimal rate = BigDecimal.valueOf(accepted != null ? accepted : 0L)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
        return new AcceptanceRateResponse(total, accepted != null ? accepted : 0L, rate);
    }

    /**
     * Évolution mensuelle des offres créées (pour une année donnée).
     */
    public MonthlyEvolutionResponse getMonthlyEvolution(Long freelancerId, int year) {
        List<MonthlyEvolutionResponse.MonthCount> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            LocalDateTime startDt = start.atStartOfDay();
            LocalDateTime endDt = end.atTime(23, 59, 59, 999_999_999);
            Long count = offerRepository.countByFreelancerIdAndCreatedAtBetween(freelancerId, startDt, endDt);
            months.add(new MonthlyEvolutionResponse.MonthCount(month, count != null ? count : 0L));
        }
        return new MonthlyEvolutionResponse(year, months);
    }

    /**
     * Traduction d'une offre (titre + description) via Google Translate.
     * Langues supportées : fr, en, ar.
     */
    public TranslateOfferResponse translateOffer(Long offerId, String targetLanguage) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));
        List<String> texts = List.of(
                offer.getTitle() != null ? offer.getTitle() : "",
                offer.getDescription() != null ? offer.getDescription() : ""
        );
        List<String> translated = translationService.translate(texts, targetLanguage);
        String title = translated.size() > 0 ? translated.get(0) : offer.getTitle();
        String description = translated.size() > 1 ? translated.get(1) : offer.getDescription();
        return new TranslateOfferResponse(title, description);
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
        if (request.getDeadline() != null) {
            offer.setDeadline(request.getDeadline());
        }
        offer.setCategory(request.getCategory());
        offer.setTags(request.getTags());
        offer.setImageUrl(request.getImageUrl());
        if (request.getProjectStatusId() != null) {
            offer.setProjectStatusId(request.getProjectStatusId());
        }

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
            if (filter.getCreatedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                    filter.getCreatedAtFrom().atStartOfDay()));
            }
            if (filter.getCreatedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                    filter.getCreatedAtTo().atTime(23, 59, 59, 999_999_999)));
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

    /** Pour Smart Matching : mapper une liste d'offres vers OfferResponse. */
    public List<OfferResponse> mapOffersToResponse(List<Offer> offers) {
        if (offers == null || offers.isEmpty()) return List.of();
        return offers.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
}