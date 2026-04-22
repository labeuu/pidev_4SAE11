package com.example.review.service;

import com.example.review.client.NotificationClient;
import com.example.review.client.UserClient;
import com.example.review.dto.NotificationRequestDto;
import com.example.review.dto.UserDto;
import com.example.review.entity.Review;
import com.example.review.entity.ReviewResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewNotificationServiceTest {

    @Mock
    private NotificationClient notificationClient;
    @Mock
    private UserClient userClient;
    @Mock
    private ReviewEmailService reviewEmailService;

    @InjectMocks
    private ReviewNotificationService reviewNotificationService;

    @Test
    void notifyReviewResponseReceived_withNullResponse_doesNothing() {
        reviewNotificationService.notifyReviewResponseReceived(null);

        verify(notificationClient, never()).create(org.mockito.ArgumentMatchers.any(NotificationRequestDto.class));
        verify(userClient, never()).getUserById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void notifyReviewResponseReceived_sendsPushAndEmail() {
        Review review = new Review();
        review.setId(17L);
        review.setRevieweeId(5L);

        ReviewResponse response = new ReviewResponse();
        response.setId(30L);
        response.setRespondentId(12L);
        response.setMessage("Great collaboration and communication.");
        response.setReview(review);

        when(userClient.getUserById(5L)).thenReturn(new UserDto(5L, "reviewee@demo.tn", "Ali", "Ben"));

        reviewNotificationService.notifyReviewResponseReceived(response);

        ArgumentCaptor<NotificationRequestDto> pushCaptor = ArgumentCaptor.forClass(NotificationRequestDto.class);
        verify(notificationClient).create(pushCaptor.capture());
        NotificationRequestDto push = pushCaptor.getValue();
        assertThat(push.getUserId()).isEqualTo("5");
        assertThat(push.getType()).isEqualTo(ReviewNotificationService.TYPE_REVIEW_RESPONSE);
        assertThat(push.getData().get("reviewId")).isEqualTo("17");
        assertThat(push.getData().get("reviewResponseId")).isEqualTo("30");
        assertThat(push.getData().get("respondentId")).isEqualTo("12");

        verify(reviewEmailService).sendReviewResponseEmail(
                "reviewee@demo.tn",
                "Ali Ben",
                "Great collaboration and communication.",
                17L,
                30L
        );
    }
}
