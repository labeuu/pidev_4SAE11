package org.example.offer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.offer.dto.request.OfferApplicationRequest;
import org.example.offer.dto.response.OfferApplicationResponse;
import org.example.offer.entity.ApplicationStatus;
import org.example.offer.service.OfferApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfferApplicationController {

    private final OfferApplicationService applicationService;

    /**
     * CREATE - Postuler à une offre
     * POST /api/applications
     */
    @PostMapping
    public ResponseEntity<OfferApplicationResponse> applyToOffer(@Valid @RequestBody OfferApplicationRequest request) {
        OfferApplicationResponse response = applicationService.applyToOffer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * READ - Récupérer une candidature par ID
     * GET /api/applications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OfferApplicationResponse> getApplicationById(@PathVariable Long id) {
        OfferApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les candidatures d'une offre
     * GET /api/applications/offer/{offerId}?page=0&size=10
     */
    @GetMapping("/offer/{offerId}")
    public ResponseEntity<Page<OfferApplicationResponse>> getApplicationsByOffer(
            @PathVariable Long offerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OfferApplicationResponse> response = applicationService.getApplicationsByOffer(offerId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les candidatures d'un client
     * GET /api/applications/client/{clientId}?page=0&size=10
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<OfferApplicationResponse>> getApplicationsByClient(
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OfferApplicationResponse> response = applicationService.getApplicationsByClient(clientId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les candidatures en attente
     * GET /api/applications/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<OfferApplicationResponse>> getPendingApplications() {
        List<OfferApplicationResponse> response = applicationService.getPendingApplications();
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les candidatures non lues d'un freelancer
     * GET /api/applications/unread/freelancer/{freelancerId}
     */
    @GetMapping("/unread/freelancer/{freelancerId}")
    public ResponseEntity<List<OfferApplicationResponse>> getUnreadApplicationsByFreelancer(@PathVariable Long freelancerId) {
        List<OfferApplicationResponse> response = applicationService.getUnreadApplicationsByFreelancer(freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Récupérer les candidatures d'une offre par statut
     * GET /api/applications/offer/{offerId}/status/{status}
     */
    @GetMapping("/offer/{offerId}/status/{status}")
    public ResponseEntity<List<OfferApplicationResponse>> getApplicationsByOfferAndStatus(
            @PathVariable Long offerId,
            @PathVariable ApplicationStatus status) {
        List<OfferApplicationResponse> response = applicationService.getApplicationsByOfferAndStatus(offerId, status);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Compter les candidatures en attente pour une offre
     * GET /api/applications/offer/{offerId}/pending/count
     */
    @GetMapping("/offer/{offerId}/pending/count")
    public ResponseEntity<Long> countPendingApplications(@PathVariable Long offerId) {
        Long count = applicationService.countPendingApplications(offerId);
        return ResponseEntity.ok(count);
    }

    /**
     * READ - Récupérer les candidatures récentes
     * GET /api/applications/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<OfferApplicationResponse>> getRecentApplications() {
        List<OfferApplicationResponse> response = applicationService.getRecentApplications();
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Accepter une candidature
     * PATCH /api/applications/{id}/accept?freelancerId=123
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<OfferApplicationResponse> acceptApplication(
            @PathVariable Long id,
            @RequestParam Long freelancerId) {
        OfferApplicationResponse response = applicationService.acceptApplication(id, freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Rejeter une candidature
     * PATCH /api/applications/{id}/reject?freelancerId=123&reason=...
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<OfferApplicationResponse> rejectApplication(
            @PathVariable Long id,
            @RequestParam Long freelancerId,
            @RequestParam(required = false) String reason) {
        OfferApplicationResponse response = applicationService.rejectApplication(id, freelancerId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Mettre en liste courte
     * PATCH /api/applications/{id}/shortlist?freelancerId=123
     */
    @PatchMapping("/{id}/shortlist")
    public ResponseEntity<OfferApplicationResponse> shortlistApplication(
            @PathVariable Long id,
            @RequestParam Long freelancerId) {
        OfferApplicationResponse response = applicationService.shortlistApplication(id, freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Marquer comme lu
     * PATCH /api/applications/{id}/mark-read?freelancerId=123
     */
    @PatchMapping("/{id}/mark-read")
    public ResponseEntity<OfferApplicationResponse> markAsRead(
            @PathVariable Long id,
            @RequestParam Long freelancerId) {
        OfferApplicationResponse response = applicationService.markAsRead(id, freelancerId);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Retirer une candidature
     * PATCH /api/applications/{id}/withdraw?clientId=456
     */
    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<OfferApplicationResponse> withdrawApplication(
            @PathVariable Long id,
            @RequestParam Long clientId) {
        OfferApplicationResponse response = applicationService.withdrawApplication(id, clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Modifier une candidature
     * PUT /api/applications/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<OfferApplicationResponse> updateApplication(
            @PathVariable Long id,
            @Valid @RequestBody OfferApplicationRequest request) {
        OfferApplicationResponse response = applicationService.updateApplication(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE - Supprimer une candidature
     * DELETE /api/applications/{id}?clientId=456
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id,
            @RequestParam Long clientId) {
        applicationService.deleteApplication(id, clientId);
        return ResponseEntity.noContent().build();
    }
}