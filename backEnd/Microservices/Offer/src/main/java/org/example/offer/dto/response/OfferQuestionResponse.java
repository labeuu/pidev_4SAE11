package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferQuestionResponse {

    private Long id;
    private Long offerId;
    private Long clientId;
    private String questionText;
    private String answerText;
    private LocalDateTime askedAt;
    private LocalDateTime answeredAt;
    private boolean answered;
}
