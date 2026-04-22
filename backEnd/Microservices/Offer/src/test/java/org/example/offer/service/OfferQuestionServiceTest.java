package org.example.offer.service;

import org.example.offer.client.UserFeignClient;
import org.example.offer.dto.request.AnswerQuestionRequest;
import org.example.offer.dto.request.OfferQuestionRequest;
import org.example.offer.dto.response.OfferQuestionResponse;
import org.example.offer.entity.NotificationType;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferQuestion;
import org.example.offer.exception.BadRequestException;
import org.example.offer.repository.OfferQuestionRepository;
import org.example.offer.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OfferQuestionServiceTest {

    @Mock
    private OfferQuestionRepository questionRepository;
    @Mock
    private OfferRepository offerRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;
    @Mock
    private UserFeignClient userFeignClient;

    @InjectMocks
    private OfferQuestionService offerQuestionService;

    @Test
    void addQuestion_onOwnOffer_throwsBadRequest() {
        Offer offer = new Offer();
        offer.setId(10L);
        offer.setFreelancerId(8L);
        when(offerRepository.findById(10L)).thenReturn(Optional.of(offer));

        OfferQuestionRequest req = new OfferQuestionRequest("Question valid length");

        assertThatThrownBy(() -> offerQuestionService.addQuestion(10L, 8L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("own offer");
    }

    @Test
    void addQuestion_success_savesQuestionAndTriggersNotifications() {
        Offer offer = new Offer();
        offer.setId(11L);
        offer.setFreelancerId(50L);
        offer.setTitle("Backend API");
        when(offerRepository.findById(11L)).thenReturn(Optional.of(offer));
        when(userFeignClient.getUserById(50L))
                .thenReturn(Map.of("firstName", "Sarra", "email", "sarra@demo.tn"));
        when(questionRepository.save(any(OfferQuestion.class))).thenAnswer(inv -> {
            OfferQuestion q = inv.getArgument(0);
            q.setId(99L);
            return q;
        });

        OfferQuestionResponse out = offerQuestionService.addQuestion(11L, 7L, new OfferQuestionRequest("  Need delivery in 7 days?  "));

        assertThat(out.getId()).isEqualTo(99L);
        assertThat(out.getOfferId()).isEqualTo(11L);
        assertThat(out.getClientId()).isEqualTo(7L);
        assertThat(out.getQuestionText()).isEqualTo("Need delivery in 7 days?");

        ArgumentCaptor<OfferQuestion> questionCaptor = ArgumentCaptor.forClass(OfferQuestion.class);
        verify(questionRepository).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getQuestionText()).isEqualTo("Need delivery in 7 days?");

        verify(notificationService).createNotification(
                50L,
                NotificationType.NEW_QUESTION,
                "New question on your offer",
                "A client asked: \"Need delivery in 7 days?\"",
                11L,
                99L
        );
        verify(emailService).sendNewQuestionEmail("sarra@demo.tn", "Sarra", "Backend API", "Need delivery in 7 days?", 11L);
    }

    @Test
    void answerQuestion_notOwner_throwsBadRequest() {
        Offer offer = new Offer();
        offer.setId(1L);
        offer.setFreelancerId(20L);
        OfferQuestion q = new OfferQuestion();
        q.setId(5L);
        q.setOffer(offer);
        when(questionRepository.findById(5L)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> offerQuestionService.answerQuestion(5L, 99L, new AnswerQuestionRequest("answer")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("offer owner");
    }
}
