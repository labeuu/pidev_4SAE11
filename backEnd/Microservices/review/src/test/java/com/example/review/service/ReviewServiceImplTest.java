package com.example.review.service;

import com.example.review.dto.ReviewStats;
import com.example.review.entity.Review;
import com.example.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    ReviewServiceImpl reviewService;

    @Test
    void createReview_delegatesToRepository() {
        Review in = new Review();
        Review saved = new Review();
        saved.setId(1L);
        when(reviewRepository.save(in)).thenReturn(saved);

        Review out = reviewService.createReview(in);

        assertThat(out.getId()).isEqualTo(1L);
        verify(reviewRepository).save(in);
    }

    @Test
    void deleteReview_delegatesToRepository() {
        reviewService.deleteReview(9L);
        verify(reviewRepository).deleteById(9L);
    }

    @Test
    void updateReview_whenMissing_throws() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());

        Review patch = new Review();
        patch.setRating(5);

        assertThatThrownBy(() -> reviewService.updateReview(1L, patch))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Review not found with id: 1");
    }

    @Test
    void updateReview_whenFound_updatesAndSaves() {
        Review existing = new Review();
        existing.setId(2L);
        existing.setReviewerId(1L);
        existing.setRevieweeId(2L);
        existing.setProjectId(3L);
        existing.setRating(3);
        existing.setComment("old");
        when(reviewRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing)).thenReturn(existing);

        Review patch = new Review();
        patch.setReviewerId(10L);
        patch.setRevieweeId(20L);
        patch.setProjectId(30L);
        patch.setRating(5);
        patch.setComment("new");

        Review result = reviewService.updateReview(2L, patch);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("new");
        assertThat(result.getReviewerId()).isEqualTo(10L);
        verify(reviewRepository).save(existing);
    }

    @Test
    void getPage_clampsSize_whenZeroUsesTen() {
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        reviewService.getPage(null, null, 0, 0);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void getPage_clampsSize_maxHundred() {
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        reviewService.getPage(null, null, 0, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getStats_buildsCountsAndAverage() {
        List<Object[]> rows = List.of(
                new Object[]{4, 2L},
                new Object[]{5, 1L}
        );
        when(reviewRepository.ratingCountsByReviewerAndReviewee(null, null)).thenReturn(rows);

        ReviewStats stats = reviewService.getStats();

        assertThat(stats.getTotalCount()).isEqualTo(3L);
        assertThat(stats.getAverageRating()).isEqualTo(4.33);
        assertThat(stats.getCountByRating().get(4)).isEqualTo(2L);
        assertThat(stats.getCountByRating().get(5)).isEqualTo(1L);
        assertThat(stats.getCountByRating().get(1)).isEqualTo(0L);
    }
}
