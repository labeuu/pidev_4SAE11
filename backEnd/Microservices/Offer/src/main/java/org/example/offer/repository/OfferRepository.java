package org.example.offer.repository;

import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {

    // ========== Recherches de base ==========
    List<Offer> findByFreelancerId(Long freelancerId);

    List<Offer> findByOfferStatus(OfferStatus status);

    Page<Offer> findByOfferStatus(OfferStatus status, Pageable pageable);

    List<Offer> findByDomain(String domain);

    Page<Offer> findByDomain(String domain, Pageable pageable);

    List<Offer> findByFreelancerIdAndOfferStatus(Long freelancerId, OfferStatus status);

    // ========== Recherches avancées ==========

    @Query("SELECT o FROM Offer o WHERE o.isActive = true AND o.offerStatus = 'AVAILABLE' ORDER BY o.createdAt DESC")
    Page<Offer> findActiveOffers(Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.isFeatured = true AND o.isActive = true ORDER BY o.publishedAt DESC")
    List<Offer> findFeaturedOffers();

    @Query("SELECT o FROM Offer o WHERE o.offerStatus = 'AVAILABLE' AND o.isActive = true AND o.rating >= :minRating ORDER BY o.rating DESC")
    Page<Offer> findTopRatedOffers(@Param("minRating") BigDecimal minRating, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.offerStatus = 'AVAILABLE' AND o.isActive = true AND o.domain = :domain ORDER BY o.publishedAt DESC")
    Page<Offer> findAvailableOffersByDomain(@Param("domain") String domain, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.price BETWEEN :minPrice AND :maxPrice AND o.offerStatus = 'AVAILABLE' AND o.isActive = true")
    Page<Offer> findByPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.title LIKE %:keyword% OR o.description LIKE %:keyword% OR o.tags LIKE %:keyword%")
    Page<Offer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT o FROM Offer o WHERE o.deadline >= :date AND o.offerStatus = 'AVAILABLE'")
    List<Offer> findOffersExpiringSoon(@Param("date") LocalDateTime date);

    @Query("SELECT o FROM Offer o WHERE o.deadline < :now AND o.offerStatus = 'AVAILABLE'")
    List<Offer> findExpiredOffers(@Param("now") LocalDateTime now);

    // ========== Statistiques ==========

    @Query("SELECT COUNT(o) FROM Offer o WHERE o.freelancerId = :freelancerId AND o.offerStatus = :status")
    Long countByFreelancerIdAndStatus(@Param("freelancerId") Long freelancerId, @Param("status") OfferStatus status);

    @Query("SELECT AVG(o.rating) FROM Offer o WHERE o.freelancerId = :freelancerId")
    BigDecimal calculateAverageRating(@Param("freelancerId") Long freelancerId);

    @Query("SELECT SUM(o.price) FROM Offer o WHERE o.freelancerId = :freelancerId AND o.offerStatus = 'ACCEPTED'")
    BigDecimal calculateTotalRevenue(@Param("freelancerId") Long freelancerId);

    @Query("SELECT o FROM Offer o WHERE o.freelancerId = :freelancerId ORDER BY o.viewsCount DESC")
    Page<Offer> findMostViewedByFreelancer(@Param("freelancerId") Long freelancerId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Offer o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    Long countOffersCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ========== Recherches avec ProjectStatus ==========

    @Query("SELECT o FROM Offer o WHERE o.projectStatus.code = :statusCode")
    List<Offer> findByProjectStatusCode(@Param("statusCode") String statusCode);

    @Query("SELECT o FROM Offer o WHERE o.projectStatus.id = :statusId AND o.isActive = true")
    Page<Offer> findActiveOffersByProjectStatus(@Param("statusId") Long statusId, Pageable pageable);
}