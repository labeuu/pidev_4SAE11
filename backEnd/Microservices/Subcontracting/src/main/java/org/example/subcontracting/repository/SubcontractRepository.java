package org.example.subcontracting.repository;

import org.example.subcontracting.entity.Subcontract;
import org.example.subcontracting.entity.SubcontractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractRepository extends JpaRepository<Subcontract, Long> {

    List<Subcontract> findByMainFreelancerIdOrderByCreatedAtDesc(Long mainFreelancerId);

    List<Subcontract> findBySubcontractorIdOrderByCreatedAtDesc(Long subcontractorId);

    List<Subcontract> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Subcontract> findByStatusOrderByCreatedAtDesc(SubcontractStatus status);

    List<Subcontract> findByMainFreelancerIdAndStatus(Long mainFreelancerId, SubcontractStatus status);

    List<Subcontract> findBySubcontractorIdAndStatus(Long subcontractorId, SubcontractStatus status);

    long countByMainFreelancerId(Long mainFreelancerId);

    long countBySubcontractorId(Long subcontractorId);

    long countByProjectId(Long projectId);

    long countByStatus(SubcontractStatus status);
}
