package org.example.offer.repository;

import org.example.offer.entity.OfferView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferViewRepository extends JpaRepository<OfferView, Long> {

    List<OfferView> findByClientIdOrderByViewedAtDesc(Long clientId, Pageable pageable);

    @Query("SELECT v FROM OfferView v JOIN FETCH v.offer WHERE v.clientId = :clientId AND v.viewedAt >= :since ORDER BY v.viewedAt DESC")
    List<OfferView> findByClientIdAndViewedAtAfter(@Param("clientId") Long clientId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(v) FROM OfferView v WHERE v.clientId = :clientId AND v.offer.id = :offerId")
    long countByClientIdAndOfferId(@Param("clientId") Long clientId, @Param("offerId") Long offerId);
}
