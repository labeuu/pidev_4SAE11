package org.example.vendor.repository;

import org.example.vendor.entity.VendorApproval;
import org.example.vendor.entity.VendorApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorApprovalRepository extends JpaRepository<VendorApproval, Long> {

    List<VendorApproval> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    List<VendorApproval> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<VendorApproval> findByStatus(VendorApprovalStatus status);

    Optional<VendorApproval> findByOrganizationIdAndFreelancerIdAndDomain(
            Long organizationId, Long freelancerId, String domain);

    Optional<VendorApproval> findByOrganizationIdAndFreelancerId(
            Long organizationId, Long freelancerId);

    @Query("SELECT v FROM VendorApproval v WHERE v.status = 'APPROVED' " +
           "AND v.nextReviewDate IS NOT NULL AND v.nextReviewDate <= :date")
    List<VendorApproval> findReviewsDueBefore(@Param("date") LocalDate date);

    /** MÉTIER 2 — Révisions déjà dépassées (nextReviewDate strictement avant aujourd'hui). */
    @Query("SELECT v FROM VendorApproval v WHERE v.status = 'APPROVED' " +
           "AND v.nextReviewDate IS NOT NULL AND v.nextReviewDate < :today")
    List<VendorApproval> findReviewsOverdue(@Param("today") LocalDate today);

    @Query("SELECT v FROM VendorApproval v WHERE v.status = 'APPROVED' " +
           "AND v.validUntil IS NOT NULL AND v.validUntil < :today")
    List<VendorApproval> findExpiredApprovals(@Param("today") LocalDate today);

    /** MÉTIER 5 — Agréments actifs dont la validité expire exactement ce jour (rappel J-30). */
    @Query("SELECT v FROM VendorApproval v WHERE v.status = 'APPROVED' AND v.validUntil = :day "
           + "AND v.expiryReminderSentAt IS NULL")
    List<VendorApproval> findApprovedExpiringOn(@Param("day") LocalDate day);

    /** MÉTIER 5 — Liste admin : expire entre demain et today+days (inclus). */
    @Query("SELECT v FROM VendorApproval v WHERE v.status = 'APPROVED' AND v.validUntil IS NOT NULL "
           + "AND v.validUntil > :today AND v.validUntil <= :until")
    List<VendorApproval> findApprovedExpiringWithin(@Param("today") LocalDate today, @Param("until") LocalDate until);

    @Query("SELECT COUNT(v) FROM VendorApproval v WHERE v.organizationId = :orgId AND v.status = 'APPROVED'")
    long countApprovedByOrganization(@Param("orgId") Long organizationId);

    @Query("SELECT COUNT(v) FROM VendorApproval v WHERE v.freelancerId = :fId AND v.status = 'APPROVED'")
    long countApprovedByFreelancer(@Param("fId") Long freelancerId);

    boolean existsByOrganizationIdAndFreelancerIdAndStatus(
            Long organizationId, Long freelancerId, VendorApprovalStatus status);

    // ── Trust score queries ───────────────────────────────────

    List<VendorApproval> findByFreelancerId(Long freelancerId);

    @Query("SELECT COALESCE(SUM(v.reviewCount), 0) FROM VendorApproval v WHERE v.freelancerId = :fId")
    long sumReviewCountByFreelancer(@Param("fId") Long freelancerId);

    @Query("SELECT COUNT(v) FROM VendorApproval v WHERE v.freelancerId = :fId AND v.status = 'REJECTED'")
    long countRejectedByFreelancer(@Param("fId") Long freelancerId);

    @Query("SELECT COUNT(v) FROM VendorApproval v WHERE v.freelancerId = :fId AND v.status = 'SUSPENDED'")
    long countSuspendedByFreelancer(@Param("fId") Long freelancerId);

    // ── Multi-agreement support ───────────────────────────────

    @Query("SELECT v FROM VendorApproval v WHERE v.organizationId = :orgId AND v.freelancerId = :fId "
           + "AND v.domain = :domain AND v.status IN ('PENDING', 'APPROVED')")
    Optional<VendorApproval> findActiveDuplicate(
            @Param("orgId") Long organizationId,
            @Param("fId") Long freelancerId,
            @Param("domain") String domain);

    // ── AI Dashboard queries ──────────────────────────────────

    long countByStatus(VendorApprovalStatus status);

    @Query("SELECT v FROM VendorApproval v WHERE v.createdAt >= :since")
    List<VendorApproval> findCreatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT v.domain, COUNT(v), SUM(CASE WHEN v.status = 'APPROVED' AND (v.validUntil IS NULL OR v.validUntil >= CURRENT_DATE) THEN 1 ELSE 0 END) "
           + "FROM VendorApproval v WHERE v.domain IS NOT NULL GROUP BY v.domain ORDER BY COUNT(v) DESC")
    List<Object[]> countGroupedByDomain();

    @Query("SELECT v.professionalSector, COUNT(v), SUM(CASE WHEN v.status = 'APPROVED' AND (v.validUntil IS NULL OR v.validUntil >= CURRENT_DATE) THEN 1 ELSE 0 END) "
           + "FROM VendorApproval v WHERE v.professionalSector IS NOT NULL GROUP BY v.professionalSector ORDER BY COUNT(v) DESC")
    List<Object[]> countGroupedBySector();
}
