package org.example.offer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.AnswerQuestionRequest;
import org.example.offer.dto.request.OfferFilterRequest;
import org.example.offer.dto.request.OfferQuestionRequest;
import org.example.offer.dto.request.OfferRequest;
import org.example.offer.dto.request.TranslateTextsRequest;
import org.example.offer.dto.response.AcceptanceRateResponse;
import org.example.offer.dto.response.MonthlyEvolutionResponse;
import org.example.offer.dto.response.OfferResponse;
import org.example.offer.dto.response.OfferStatsResponse;
import org.example.offer.dto.response.OffersByStatusResponse;
import org.example.offer.dto.response.TranslateOfferResponse;
import org.example.offer.dto.response.OfferQuestionResponse;
import org.example.offer.dto.response.TranslateTextsResponse;
import org.example.offer.entity.OfferStatus;
import org.example.offer.service.OfferQuestionService;
import org.example.offer.service.OfferService;
import org.example.offer.service.SmartMatchingService;
import org.example.offer.service.TranslationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferController {

    private final OfferService offerService;
    private final TranslationService translationService;
    private final SmartMatchingService smartMatchingService;
    private final OfferQuestionService offerQuestionService;

    /**
     * CREATE - Créer une nouvelle offre
     * POST /api/offers
     */
    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(@Valid @RequestBody OfferRequest request) {
        OfferResponse response = offerService.createOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * READ - Récupérer une offre par ID
     * GET /api/offers/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OfferResponse> getOfferById(@PathVariable Long id) {
        OfferResponse response = offerService.getOfferById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer toutes les offres actives (avec pagination)
     * GET /api/offers?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<OfferResponse>> getActiveOffers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OfferResponse> response = offerService.getActiveOffers(page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les offres d'un freelancer
     * GET /api/offers/freelancer/{freelancerId}
     */
    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<List<OfferResponse>> getOffersByFreelancer(@PathVariable Long freelancerId) {
        List<OfferResponse> response = offerService.getOffersByFreelancer(freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les offres featured
     * GET /api/offers/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<List<OfferResponse>> getFeaturedOffers() {
        List<OfferResponse> response = offerService.getFeaturedOffers();
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les offres les mieux notées
     * GET /api/offers/top-rated?minRating=4.0&page=0&size=10
     */
    @GetMapping("/top-rated")
    public ResponseEntity<Page<OfferResponse>> getTopRatedOffers(
            @RequestParam(defaultValue = "4.0") BigDecimal minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OfferResponse> response = offerService.getTopRatedOffers(minRating, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Recherche avancée avec filtres
     * POST /api/offers/search
     */
    @PostMapping("/search")
    public ResponseEntity<Page<OfferResponse>> searchOffers(@RequestBody OfferFilterRequest filter) {
        Page<OfferResponse> response = offerService.searchOffers(filter);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Statistiques d'un freelancer
     * GET /api/offers/stats/freelancer/{freelancerId}
     */
    @GetMapping("/stats/freelancer/{freelancerId}")
    public ResponseEntity<OfferStatsResponse> getFreelancerStats(@PathVariable Long freelancerId) {
        OfferStatsResponse response = offerService.getFreelancerStats(freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Nombre d'offres par statut (agrégation backend)
     * GET /api/offers/stats/freelancer/{freelancerId}/by-status
     */
    @GetMapping("/stats/freelancer/{freelancerId}/by-status")
    public ResponseEntity<OffersByStatusResponse> getOffersCountByStatus(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(offerService.getOffersCountByStatus(freelancerId));
    }

    /**
     * READ - Taux d'acceptation des candidatures
     * GET /api/offers/stats/freelancer/{freelancerId}/acceptance-rate
     */
    @GetMapping("/stats/freelancer/{freelancerId}/acceptance-rate")
    public ResponseEntity<AcceptanceRateResponse> getAcceptanceRate(@PathVariable Long freelancerId) {
        return ResponseEntity.ok(offerService.getAcceptanceRate(freelancerId));
    }

    /**
     * READ - Évolution mensuelle des offres
     * GET /api/offers/stats/freelancer/{freelancerId}/monthly-evolution?year=2025
     */
    @GetMapping("/stats/freelancer/{freelancerId}/monthly-evolution")
    public ResponseEntity<MonthlyEvolutionResponse> getMonthlyEvolution(
            @PathVariable Long freelancerId,
            @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(offerService.getMonthlyEvolution(freelancerId, year));
    }

    /**
     * UPDATE - Mettre à jour une offre
     * PUT /api/offers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody OfferRequest request) {
        OfferResponse response = offerService.updateOffer(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Publier une offre
     * PATCH /api/offers/{id}/publish?freelancerId=123
     */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<OfferResponse> publishOffer(
            @PathVariable Long id,
            @RequestParam Long freelancerId) {
        OfferResponse response = offerService.publishOffer(id, freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Changer le statut d'une offre
     * PATCH /api/offers/{id}/status?status=ACCEPTED&freelancerId=123
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OfferResponse> changeOfferStatus(
            @PathVariable Long id,
            @RequestParam OfferStatus status,
            @RequestParam Long freelancerId) {
        OfferResponse response = offerService.changeOfferStatus(id, status, freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Mettre à jour les scores
     * PATCH /api/offers/{id}/scores?rating=4.5&communicationScore=4.8
     */
    @PatchMapping("/{id}/scores")
    public ResponseEntity<OfferResponse> updateScores(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal rating,
            @RequestParam(required = false) BigDecimal communicationScore) {
        OfferResponse response = offerService.updateScores(id, rating, communicationScore);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE - Supprimer une offre
     * DELETE /api/offers/{id}?freelancerId=123
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(
            @PathVariable Long id,
            @RequestParam Long freelancerId) {
        offerService.deleteOffer(id, freelancerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Traduction d'une offre (titre + description) – FR / EN / AR
     * POST /api/offers/{id}/translate
     */
    @PostMapping("/{id}/translate")
    public ResponseEntity<TranslateOfferResponse> translateOffer(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String targetLanguage = body != null && body.containsKey("targetLanguage")
                ? body.get("targetLanguage") : "en";
        TranslateOfferResponse response = offerService.translateOffer(id, targetLanguage);
        return ResponseEntity.ok(response);
    }

    /**
     * Traduction de textes libres (pour formulaire add/edit) – FR / EN / AR
     * POST /api/offers/translate-texts
     */
    @PostMapping("/translate-texts")
    public ResponseEntity<TranslateTextsResponse> translateTexts(@Valid @RequestBody TranslateTextsRequest request) {
        java.util.List<String> translations = translationService.translate(request.getTexts(), request.getTargetLanguage());
        return ResponseEntity.ok(new TranslateTextsResponse(translations));
    }

    /**
     * Smart Matching : recommandations d'offres pour un client (historique + comportement)
     * GET /api/offers/recommendations/client/{clientId}?limit=20
     */
    @GetMapping("/recommendations/client/{clientId}")
    public ResponseEntity<List<OfferResponse>> getRecommendedOffers(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "20") int limit) {
        List<OfferResponse> list = smartMatchingService.getRecommendedOffersForClient(clientId, limit);
        return ResponseEntity.ok(list);
    }

    /**
     * Enregistrer une vue d'offre par un client (pour le comportement / Smart Matching)
     * POST /api/offers/views
     */
    @PostMapping("/views")
    public ResponseEntity<Void> recordOfferView(@RequestBody Map<String, Long> body) {
        Long clientId = body != null ? body.get("clientId") : null;
        Long offerId = body != null ? body.get("offerId") : null;
        smartMatchingService.recordView(clientId, offerId);
        return ResponseEntity.noContent().build();
    }

    // ——— Q&A (questions-réponses clients avant de commander) ———

    /**
     * Liste des questions-réponses pour une offre
     * GET /api/offers/{offerId}/questions
     */
    @GetMapping("/{offerId}/questions")
    public ResponseEntity<List<OfferQuestionResponse>> getOfferQuestions(@PathVariable Long offerId) {
        List<OfferQuestionResponse> list = offerQuestionService.getQuestionsByOfferId(offerId);
        return ResponseEntity.ok(list);
    }

    /**
     * Un client pose une question sur une offre
     * POST /api/offers/{offerId}/questions (body: { questionText }, header or body: clientId)
     */
    @PostMapping("/{offerId}/questions")
    public ResponseEntity<OfferQuestionResponse> addOfferQuestion(
            @PathVariable Long offerId,
            @RequestParam Long clientId,
            @Valid @RequestBody OfferQuestionRequest request) {
        OfferQuestionResponse response = offerQuestionService.addQuestion(offerId, clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Le freelancer répond à une question
     * PATCH /api/offers/questions/{questionId}/answer?freelancerId=... (body: { answerText })
     */
    @PatchMapping("/questions/{questionId}/answer")
    public ResponseEntity<OfferQuestionResponse> answerOfferQuestion(
            @PathVariable Long questionId,
            @RequestParam Long freelancerId,
            @Valid @RequestBody AnswerQuestionRequest request) {
        OfferQuestionResponse response = offerQuestionService.answerQuestion(questionId, freelancerId, request);
        return ResponseEntity.ok(response);
    }
}