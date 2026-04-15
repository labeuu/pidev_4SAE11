package org.example.subcontracting.coach.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.coach.CoachWalletService;
import org.example.subcontracting.coach.dto.FeatureCostPatchRequest;
import org.example.subcontracting.coach.entity.CoachFeatureCost;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/coach/feature-costs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CoachFeatureCostController {

    private final CoachWalletService coachWalletService;

    @GetMapping
    public ResponseEntity<List<CoachFeatureCost>> list() {
        return ResponseEntity.ok(coachWalletService.listFeatureCosts());
    }

    @PatchMapping("/{code}")
    public ResponseEntity<CoachFeatureCost> patch(
            @RequestParam Long adminUserId,
            @PathVariable String code,
            @Valid @RequestBody FeatureCostPatchRequest body) {
        if (adminUserId == null || adminUserId <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "adminUserId requis");
        }
        return ResponseEntity.ok(coachWalletService.updateFeatureCost(code, body.getCostPoints(), adminUserId));
    }
}
