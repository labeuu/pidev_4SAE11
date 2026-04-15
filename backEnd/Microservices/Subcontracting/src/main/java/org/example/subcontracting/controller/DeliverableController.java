package org.example.subcontracting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.dto.request.DeliverableRequest;
import org.example.subcontracting.dto.request.DeliverableReviewRequest;
import org.example.subcontracting.dto.request.DeliverableSubmitRequest;
import org.example.subcontracting.dto.response.DeliverableResponse;
import org.example.subcontracting.service.SubcontractService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcontracts/{subcontractId}/deliverables")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DeliverableController {

    private final SubcontractService service;

    @PostMapping
    public ResponseEntity<DeliverableResponse> addDeliverable(
            @PathVariable Long subcontractId,
            @Valid @RequestBody DeliverableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addDeliverable(subcontractId, request));
    }

    @GetMapping
    public ResponseEntity<List<DeliverableResponse>> getDeliverables(@PathVariable Long subcontractId) {
        return ResponseEntity.ok(service.getDeliverables(subcontractId));
    }

    @PutMapping("/{deliverableId}")
    public ResponseEntity<DeliverableResponse> updateDeliverable(
            @PathVariable Long subcontractId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody DeliverableRequest request) {
        return ResponseEntity.ok(service.updateDeliverable(deliverableId, request));
    }

    @DeleteMapping("/{deliverableId}")
    public ResponseEntity<Void> deleteDeliverable(
            @PathVariable Long subcontractId,
            @PathVariable Long deliverableId) {
        service.deleteDeliverable(deliverableId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{deliverableId}/submit")
    public ResponseEntity<DeliverableResponse> submitDeliverable(
            @PathVariable Long subcontractId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody DeliverableSubmitRequest request) {
        return ResponseEntity.ok(service.submitDeliverable(deliverableId, request));
    }

    @PatchMapping("/{deliverableId}/review")
    public ResponseEntity<DeliverableResponse> reviewDeliverable(
            @PathVariable Long subcontractId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody DeliverableReviewRequest request) {
        return ResponseEntity.ok(service.reviewDeliverable(deliverableId, request));
    }
}
