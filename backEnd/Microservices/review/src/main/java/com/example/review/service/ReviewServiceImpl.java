package com.example.review.service;

import com.example.review.dto.ReviewPageResponse;
import com.example.review.dto.ReviewStats;
import com.example.review.entity.Review;
import com.example.review.repository.ReviewRepository;
import com.example.review.repository.ReviewSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public Review createReview(Review review) {
        return reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> getReviewById(Long id) {
        return reviewRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByReviewerId(Long reviewerId) {
        return reviewRepository.findByReviewerId(reviewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByRevieweeId(Long revieweeId) {
        return reviewRepository.findByRevieweeId(revieweeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByProjectId(Long projectId) {
        return reviewRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByRevieweeAndProject(Long revieweeId, Long projectId) {
        return reviewRepository.findByRevieweeIdAndProjectId(revieweeId, projectId);
    }

    @Override
    public Review updateReview(Long id, Review review) {
        return reviewRepository.findById(id)
                .map(existingReview -> {
                    existingReview.setReviewerId(review.getReviewerId());
                    existingReview.setRevieweeId(review.getRevieweeId());
                    existingReview.setProjectId(review.getProjectId());
                    existingReview.setRating(review.getRating());
                    existingReview.setComment(review.getComment());
                    return reviewRepository.save(existingReview);
                })
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));
    }

    @Override
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPageResponse getPage(String search, Integer rating, int page, int size) {
        return getPageInternal(search, rating, null, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPageResponse getPageByReviewerId(Long reviewerId, String search, Integer rating, int page, int size) {
        return getPageInternal(search, rating, reviewerId, null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPageResponse getPageByRevieweeId(Long revieweeId, String search, Integer rating, int page, int size) {
        return getPageInternal(search, rating, null, revieweeId, page, size);
    }

    private ReviewPageResponse getPageInternal(String search, Integer rating, Long reviewerId, Long revieweeId, int page, int size) {
        int safeSize = Math.min(Math.max(size <= 0 ? 10 : size, 1), 100);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Review> spec = ReviewSpecs.withSearch(search, rating, reviewerId, revieweeId);
        var springPage = reviewRepository.findAll(spec, pageable);
        return new ReviewPageResponse(
                springPage.getContent(),
                springPage.getTotalPages(),
                springPage.getTotalElements(),
                springPage.getSize(),
                springPage.getNumber(),
                springPage.isFirst(),
                springPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStats getStats() {
        return buildStatsFromCounts(reviewRepository.ratingCountsByReviewerAndReviewee(null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStats getStatsByReviewer(Long reviewerId) {
        return buildStatsFromCounts(reviewRepository.ratingCountsByReviewerAndReviewee(reviewerId, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStats getStatsByReviewee(Long revieweeId) {
        return buildStatsFromCounts(reviewRepository.ratingCountsByReviewerAndReviewee(null, revieweeId));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStats getStatsByReviewerAndReviewee(Long reviewerId, Long revieweeId) {
        return buildStatsFromCounts(reviewRepository.ratingCountsByReviewerAndReviewee(reviewerId, revieweeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByReviewerAndReviewee(Long reviewerId, Long revieweeId) {
        return reviewRepository.findByReviewerIdAndRevieweeIdOrderByCreatedAtDesc(reviewerId, revieweeId);
    }

    private static ReviewStats buildStatsFromCounts(List<Object[]> rows) {
        Map<Integer, Long> countByRating = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            countByRating.put(i, 0L);
        }
        long total = 0;
        double sumRating = 0;
        for (Object[] row : rows) {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            countByRating.put(rating, count);
            total += count;
            sumRating += rating * count;
        }
        double avg = total == 0 ? 0.0 : sumRating / total;
        return new ReviewStats(total, Math.round(avg * 100.0) / 100.0, countByRating);
    }
}
