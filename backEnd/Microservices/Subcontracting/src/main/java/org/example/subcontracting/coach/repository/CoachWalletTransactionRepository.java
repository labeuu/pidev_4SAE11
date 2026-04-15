package org.example.subcontracting.coach.repository;

import org.example.subcontracting.coach.entity.CoachWalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachWalletTransactionRepository extends JpaRepository<CoachWalletTransaction, Long> {

    Page<CoachWalletTransaction> findByWallet_IdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Page<CoachWalletTransaction> findByPerformedByRoleIgnoreCaseOrderByCreatedAtDesc(String role, Pageable pageable);
}
