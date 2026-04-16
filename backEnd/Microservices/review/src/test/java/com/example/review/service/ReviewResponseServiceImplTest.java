package com.example.review.service;

import com.example.review.entity.Review;
import com.example.review.entity.ReviewResponse;
import com.example.review.repository.ReviewRepository;
import com.example.review.repository.ReviewResponseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewResponseServiceImplTest {

    @Mock
    ReviewResponseRepository reviewResponseRepository;
    @Mock
    ReviewRepository reviewRepository;
    @Mock
    ReviewNotificationService reviewNotificationService;

    @InjectMocks
    ReviewResponseServiceImpl reviewResponseService;

    @Test
    void createResponse_loadsReviewByReviewIdAndNotifies() {
        Review review = new Review();
        review.setId(7L);
        review.setRevieweeId(99L);

        ReviewResponse input = new ReviewResponse();
        input.setReviewId(7L);
        input.setRespondentId(5L);
        input.setMessage("Thanks!");

        when(reviewRepository.findById(7L)).thenReturn(Optional.of(review));
        when(reviewResponseRepository.save(any(ReviewResponse.class))).thenAnswer(inv -> {
            ReviewResponse r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReviewResponse saved = reviewResponseService.createResponse(input);

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getReview()).isSameAs(review);
        verify(reviewNotificationService).notifyReviewResponseReceived(saved);
    }

    @Test
    void createResponse_missingReview_throws() {
        ReviewResponse input = new ReviewResponse();
        input.setReviewId(999L);
        input.setMessage("Hi");
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewResponseService.createResponse(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found with id: 999");
    }

    @Test
    void updateResponse_blankMessage_throws() {
        assertThatThrownBy(() -> reviewResponseService.updateResponse(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message cannot be null or blank");
    }

    @Test
    void updateResponse_trimsAndSaves() {
        ReviewResponse existing = new ReviewResponse();
        existing.setId(3L);
        existing.setMessage("old");
        when(reviewResponseRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(reviewResponseRepository.save(existing)).thenReturn(existing);

        ReviewResponse out = reviewResponseService.updateResponse(3L, "  new text  ");

        assertThat(out.getMessage()).isEqualTo("new text");
        verify(reviewResponseRepository).save(existing);
    }
}
