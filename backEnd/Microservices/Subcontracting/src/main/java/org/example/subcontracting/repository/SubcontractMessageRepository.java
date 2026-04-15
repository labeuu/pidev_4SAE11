package org.example.subcontracting.repository;

import org.example.subcontracting.entity.SubcontractMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubcontractMessageRepository extends JpaRepository<SubcontractMessage, Long> {
    List<SubcontractMessage> findBySubcontractIdOrderByCreatedAtAsc(Long subcontractId);
}
