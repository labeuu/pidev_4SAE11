package org.example.subcontracting.repository;

import org.example.subcontracting.entity.SubcontractAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractAuditRepository extends JpaRepository<SubcontractAudit, Long> {

    List<SubcontractAudit> findBySubcontractIdOrderByCreatedAtDesc(Long subcontractId);

    List<SubcontractAudit> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId);

    @Query("SELECT a FROM SubcontractAudit a WHERE a.subcontractId IN " +
            "(SELECT s.id FROM Subcontract s WHERE s.mainFreelancerId = :userId OR s.subcontractorId = :userId) " +
            "ORDER BY a.createdAt DESC")
    List<SubcontractAudit> findAllByFreelancerInvolved(Long userId);
}
