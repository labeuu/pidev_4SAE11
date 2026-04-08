package org.example.vendor.repository;

import org.example.vendor.entity.VendorApprovalAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VendorApprovalAuditRepository extends JpaRepository<VendorApprovalAudit, Long> {

    List<VendorApprovalAudit> findByVendorApprovalIdOrderByCreatedAtDesc(Long vendorApprovalId);

    long countByVendorApprovalIdAndAction(Long vendorApprovalId, String action);

    // ── AI Dashboard : time-series queries ────────────────────

    @Query("SELECT a FROM VendorApprovalAudit a WHERE a.action = :action AND a.createdAt >= :since ORDER BY a.createdAt")
    List<VendorApprovalAudit> findByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);

    long countByActionAndCreatedAtBetween(String action, LocalDateTime from, LocalDateTime to);

    @Query("SELECT a.action, COUNT(a) FROM VendorApprovalAudit a WHERE a.createdAt >= :since GROUP BY a.action")
    List<Object[]> countByActionGroupedSince(@Param("since") LocalDateTime since);
}
