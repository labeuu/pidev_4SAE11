package org.example.offer.repository;

import org.example.offer.entity.ApplicationStatus;
import org.example.offer.entity.OfferApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferApplicationRepository extends JpaRepository<OfferApplication, Long> {

    List<OfferApplication> findByOfferId(Long offerId);

    Page<OfferApplication> findByOfferId(Long offerId, Pageable pageable);

    List<OfferApplication> findByClientId(Long clientId);

    Page<OfferApplication> findByClientId(Long clientId, Pageable pageable);

    List<OfferApplication> findByStatus(ApplicationStatus status);

    Page<OfferApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    List<OfferApplication> findByOfferIdAndStatus(Long offerId, ApplicationStatus status);

    @Query("SELECT a FROM OfferApplication a WHERE a.offer.freelancerId = :freelancerId AND a.status = :status")
    List<OfferApplication> findByFreelancerIdAndStatus(@Param("freelancerId") Long freelancerId, @Param("status") ApplicationStatus status);

    @Query("SELECT COUNT(a) FROM OfferApplication a WHERE a.offer.freelancerId = :freelancerId")
    Long countByOffer_FreelancerId(@Param("freelancerId") Long freelancerId);

    @Query("SELECT a FROM OfferApplication a WHERE a.offer.freelancerId = :freelancerId AND a.isRead = false ORDER BY a.appliedAt DESC")
    List<OfferApplication> findUnreadApplicationsByFreelancer(@Param("freelancerId") Long freelancerId);

    @Query("SELECT COUNT(a) FROM OfferApplication a WHERE a.offer.id = :offerId AND a.status = 'PENDING'")
    Long countPendingApplications(@Param("offerId") Long offerId);

    @Query("SELECT a FROM OfferApplication a WHERE a.appliedAt >= :date ORDER BY a.appliedAt DESC")
    List<OfferApplication> findRecentApplications(@Param("date") LocalDateTime date);

    Optional<OfferApplication> findByOfferIdAndClientId(Long offerId, Long clientId);

    boolean existsByOfferIdAndClientId(Long offerId, Long clientId);
}