package com.example.review.service;

import com.example.review.dto.ReviewPageResponse;
import com.example.review.dto.ReviewStats;
import com.example.review.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    Review createReview(Review review);

    Optional<Review> getReviewById(Long id);

    List<Review> getAllReviews();

    List<Review> getReviewsByReviewerId(Long reviewerId);

    List<Review> getReviewsByRevieweeId(Long revieweeId);

    List<Review> getReviewsByProjectId(Long projectId);

    List<Review> getReviewsByRevieweeAndProject(Long revieweeId, Long projectId);

    Review updateReview(Long id, Review review);

    void deleteReview(Long id);

    ReviewPageResponse getPage(String search, Integer rating, int page, int size);

    ReviewPageResponse getPageByReviewerId(Long reviewerId, String search, Integer rating, int page, int size);

    ReviewPageResponse getPageByRevieweeId(Long revieweeId, String search, Integer rating, int page, int size);

    ReviewStats getStats();

    ReviewStats getStatsByReviewer(Long reviewerId);

    ReviewStats getStatsByReviewee(Long revieweeId);

    /** Stats des avis pour un couple reviewer (client) ↔ reviewee (freelancer). */
    ReviewStats getStatsByReviewerAndReviewee(Long reviewerId, Long revieweeId);

    List<Review> getReviewsByReviewerAndReviewee(Long reviewerId, Long revieweeId);
}
