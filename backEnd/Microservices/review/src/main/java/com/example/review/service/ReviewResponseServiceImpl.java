package com.example.review.service;

import com.example.review.entity.Review;
import com.example.review.entity.ReviewResponse;
import com.example.review.repository.ReviewRepository;
import com.example.review.repository.ReviewResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewResponseServiceImpl implements ReviewResponseService {
    
    private final ReviewResponseRepository reviewResponseRepository;
    private final ReviewRepository reviewRepository;
    
    @Override
    public ReviewResponse createResponse(ReviewResponse reviewResponse) {
        // If a reviewId was provided in the request body, load the Review entity
        // and attach it so that the not-null FK constraint is satisfied.
        if (reviewResponse.getReview() == null && reviewResponse.getReviewId() != null) {
            Review review = reviewRepository.findById(reviewResponse.getReviewId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Review not found with id: " + reviewResponse.getReviewId()));
            reviewResponse.setReview(review);
        }
        return reviewResponseRepository.save(reviewResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getResponseById(Long id) {
        return reviewResponseRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllResponses() {
        return reviewResponseRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getResponsesByReviewId(Long reviewId) {
        return reviewResponseRepository.findByReviewIdOrderByRespondedAtAsc(reviewId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getResponsesByRespondentId(Long respondentId) {
        return reviewResponseRepository.findByRespondentId(respondentId);
    }
    
    @Override
    @Transactional
    public ReviewResponse updateResponse(Long id, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or blank");
        }
        return reviewResponseRepository.findById(id)
                .map(existingResponse -> {
                    existingResponse.setMessage(message.trim());
                    return reviewResponseRepository.save(existingResponse);
                })
                .orElseThrow(() -> new RuntimeException("ReviewResponse not found with id: " + id));
    }
    
    @Override
    public void deleteResponse(Long id) {
        reviewResponseRepository.deleteById(id);
    }
}
