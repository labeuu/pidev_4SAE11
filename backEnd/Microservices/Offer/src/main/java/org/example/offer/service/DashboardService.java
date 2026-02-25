package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import org.example.offer.dto.response.DashboardStatsResponse;
import org.example.offer.dto.response.OfferStatsResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.example.offer.repository.OfferApplicationRepository;
import org.example.offer.repository.OfferRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final OfferRepository offerRepository;
    private final OfferApplicationRepository applicationRepository;
    private final OfferService offerService;

    @Override
    public DashboardStatsResponse getFreelancerDashboardStats(Long freelancerId) {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Active Contracts (offres acceptées)
        Integer activeContracts = offerRepository
                .findByFreelancerIdAndOfferStatus(freelancerId, OfferStatus.ACCEPTED)
                .size();
        stats.setActiveContracts(activeContracts);

        // Active Contracts this week
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        long activeThisWeek = offerRepository
                .findByFreelancerId(freelancerId)
                .stream()
                .filter(offer -> offer.getCreatedAt().isAfter(oneWeekAgo))
                .filter(offer -> offer.getOfferStatus() == OfferStatus.ACCEPTED)
                .count();
        stats.setActiveContractsThisWeek((int) activeThisWeek);

        // Total Spent Last 30 Days (somme des prix des offres acceptées)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal totalSpent = offerRepository
                .findByFreelancerId(freelancerId)
                .stream()
                .filter(offer -> offer.getCreatedAt().isAfter(thirtyDaysAgo))
                .filter(offer -> offer.getOfferStatus() == OfferStatus.ACCEPTED)
                .map(offer -> offer.getPrice() != null ? offer.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalSpentLast30Days(totalSpent);

        // Total Projects Posted (lifetime)
        Integer totalProjects = offerRepository
                .findByFreelancerId(freelancerId)
                .size();
        stats.setTotalProjectsPosted(totalProjects);

        // Pending Applications
        long pendingApps = applicationRepository
                .findByStatus(ApplicationStatus.PENDING)
                .stream()
                .filter(app -> app.getOffer().getFreelancerId().equals(freelancerId))
                .count();
        stats.setPendingApplications((int) pendingApps);

        // Active Offers
        Integer activeOffers = offerRepository
                .findByFreelancerIdAndOfferStatus(freelancerId, OfferStatus.AVAILABLE)
                .size();
        stats.setActiveOffers(activeOffers);

        return stats;
    }

    @Override
    public OfferStatsResponse getOfferStatsByFreelancer(Long freelancerId) {
        return offerService.getFreelancerStats(freelancerId);
    }

    @Override
    public Long getPendingApplicationsCountByFreelancer(Long freelancerId) {
        return applicationRepository.findByStatus(ApplicationStatus.PENDING)
                .stream()
                .filter(app -> app.getOffer().getFreelancerId().equals(freelancerId))
                .count();
    }

    @Override
    public DashboardStatsResponse getFreelancerDashboardStatsForPeriod(Long freelancerId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        DashboardStatsResponse stats = new DashboardStatsResponse();
        List<Offer> offersInPeriod = offerRepository.findByFreelancerId(freelancerId).stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end))
                .toList();
        int acceptedInPeriod = (int) offersInPeriod.stream().filter(o -> o.getOfferStatus() == OfferStatus.ACCEPTED).count();
        BigDecimal spentInPeriod = offersInPeriod.stream()
                .filter(o -> o.getOfferStatus() == OfferStatus.ACCEPTED && o.getPrice() != null)
                .map(Offer::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setActiveContracts(acceptedInPeriod);
        stats.setActiveContractsThisWeek(acceptedInPeriod);
        stats.setTotalSpentLast30Days(spentInPeriod);
        stats.setTotalProjectsPosted(offersInPeriod.size());
        stats.setPendingApplications(getPendingApplicationsCountByFreelancer(freelancerId).intValue());
        stats.setActiveOffers((int) offerRepository.findByFreelancerIdAndOfferStatus(freelancerId, OfferStatus.AVAILABLE).size());
        return stats;
    }

    @Override
    public Map<Integer, BigDecimal> getRevenueByMonth(Long freelancerId, int year) {
        Map<Integer, BigDecimal> byMonth = new HashMap<>();
        for (int m = 1; m <= 12; m++) byMonth.put(m, BigDecimal.ZERO);
        LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);
        offerRepository.findByFreelancerId(freelancerId).stream()
                .filter(o -> o.getOfferStatus() == OfferStatus.ACCEPTED && o.getCreatedAt() != null
                        && !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end))
                .forEach(o -> {
                    int month = o.getCreatedAt().getMonthValue();
                    byMonth.put(month, byMonth.get(month).add(o.getPrice() != null ? o.getPrice() : BigDecimal.ZERO));
                });
        return byMonth;
    }

    @Override
    public Map<LocalDate, Long> getApplicationTrendByFreelancer(Long freelancerId, int lastDays) {
        LocalDateTime from = LocalDateTime.now().minusDays(lastDays);
        return applicationRepository.findRecentApplications(from).stream()
                .filter(a -> a.getOffer().getFreelancerId().equals(freelancerId))
                .collect(Collectors.groupingBy(a -> a.getAppliedAt().toLocalDate(), Collectors.counting()));
    }

    @Override
    public List<Long> getTopOfferIdsByViews(Long freelancerId, int limit) {
        return offerRepository.findMostViewedByFreelancer(freelancerId, PageRequest.of(0, limit))
                .getContent().stream().map(Offer::getId).toList();
    }

    @Override
    public Map<String, Object> getGlobalStatsForAdmin() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOffers", offerRepository.count());
        stats.put("totalApplications", applicationRepository.count());
        stats.put("pendingApplications", applicationRepository.findByStatus(ApplicationStatus.PENDING).size());
        return stats;
    }
}