package org.example.offer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.offer.dto.request.AnswerQuestionRequest;
import org.example.offer.dto.request.OfferQuestionRequest;
import org.example.offer.dto.response.OfferQuestionResponse;
import org.example.offer.entity.Offer;
import org.example.offer.entity.OfferQuestion;
import org.example.offer.exception.BadRequestException;
import org.example.offer.exception.ResourceNotFoundException;
import org.example.offer.repository.OfferQuestionRepository;
import org.example.offer.repository.OfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Q&A : questions des clients sur une offre, réponses du freelancer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferQuestionService {

    private final OfferQuestionRepository questionRepository;
    private final OfferRepository offerRepository;

    /**
     * Liste des questions-réponses pour une offre (ordre anti-chronologique).
     */
    @Transactional(readOnly = true)
    public List<OfferQuestionResponse> getQuestionsByOfferId(Long offerId) {
        List<OfferQuestion> list = questionRepository.findByOfferIdOrderByAskedAtDesc(offerId);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Un client pose une question sur une offre.
     */
    @Transactional
    public OfferQuestionResponse addQuestion(Long offerId, Long clientId, OfferQuestionRequest request) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + offerId));
        if (offer.getFreelancerId().equals(clientId)) {
            throw new BadRequestException("You cannot ask a question on your own offer.");
        }
        OfferQuestion q = new OfferQuestion();
        q.setOffer(offer);
        q.setClientId(clientId);
        q.setQuestionText(request.getQuestionText().trim());
        q = questionRepository.save(q);
        log.info("Question added for offer {} by client {}", offerId, clientId);
        return toResponse(q);
    }

    /**
     * Le freelancer (propriétaire de l'offre) répond à une question.
     */
    @Transactional
    public OfferQuestionResponse answerQuestion(Long questionId, Long freelancerId, AnswerQuestionRequest request) {
        OfferQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
        if (!q.getOffer().getFreelancerId().equals(freelancerId)) {
            throw new BadRequestException("Only the offer owner can answer this question.");
        }
        q.setAnswerText(request.getAnswerText().trim());
        q.setAnsweredAt(LocalDateTime.now());
        q = questionRepository.save(q);
        log.info("Question {} answered by freelancer {}", questionId, freelancerId);
        return toResponse(q);
    }

    private OfferQuestionResponse toResponse(OfferQuestion q) {
        OfferQuestionResponse r = new OfferQuestionResponse();
        r.setId(q.getId());
        r.setOfferId(q.getOffer().getId());
        r.setClientId(q.getClientId());
        r.setQuestionText(q.getQuestionText());
        r.setAnswerText(q.getAnswerText());
        r.setAskedAt(q.getAskedAt());
        r.setAnsweredAt(q.getAnsweredAt());
        r.setAnswered(q.isAnswered());
        return r;
    }
}
