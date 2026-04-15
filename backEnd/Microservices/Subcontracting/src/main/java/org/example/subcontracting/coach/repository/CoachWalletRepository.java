package org.example.subcontracting.coach.repository;

import org.example.subcontracting.coach.entity.CoachWallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CoachWalletRepository extends JpaRepository<CoachWallet, Long> {

    Optional<CoachWallet> findByUserId(Long userId);

    @Query("select w from CoachWallet w where w.blocked = true")
    List<CoachWallet> findAllBlocked();

    @Query("select w from CoachWallet w where w.balance <= :threshold and w.lowBalanceAlerted = false and w.blocked = false")
    List<CoachWallet> findLowBalanceNotAlerted(int threshold);

    Page<CoachWallet> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
