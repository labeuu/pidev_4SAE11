package com.example.review.repository;

import com.example.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    List<Review> findByReviewerId(Long reviewerId);

    List<Review> findByRevieweeId(Long revieweeId);

    List<Review> findByProjectId(Long projectId);

    List<Review> findByRevieweeIdAndProjectId(Long revieweeId, Long projectId);

    /** Avis où le reviewer (client) note le reviewee (freelancer) — couple précis. */
    List<Review> findByReviewerIdAndRevieweeIdOrderByCreatedAtDesc(Long reviewerId, Long revieweeId);

    /** Returns pairs (rating, count) for stats. Pass null for global stats. */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE (:reviewerId IS NULL OR r.reviewerId = :reviewerId) AND (:revieweeId IS NULL OR r.revieweeId = :revieweeId) GROUP BY r.rating")
    List<Object[]> ratingCountsByReviewerAndReviewee(@Param("reviewerId") Long reviewerId, @Param("revieweeId") Long revieweeId);
}
