package org.example.subcontracting.coach.repository;

import org.example.subcontracting.coach.entity.CoachInsightHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachInsightHistoryRepository extends JpaRepository<CoachInsightHistory, Long> {

    Page<CoachInsightHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
