package org.example.subcontracting.repository;

import org.example.subcontracting.entity.DeliverableStatus;
import org.example.subcontracting.entity.SubcontractDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubcontractDeliverableRepository extends JpaRepository<SubcontractDeliverable, Long> {

    List<SubcontractDeliverable> findBySubcontractIdOrderByDeadlineAsc(Long subcontractId);

    List<SubcontractDeliverable> findBySubcontractIdAndStatus(Long subcontractId, DeliverableStatus status);

    long countBySubcontractId(Long subcontractId);

    long countBySubcontractIdAndStatus(Long subcontractId, DeliverableStatus status);
}
