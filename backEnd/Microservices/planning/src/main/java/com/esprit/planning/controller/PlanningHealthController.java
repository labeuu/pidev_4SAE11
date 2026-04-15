package com.esprit.planning.controller;

import com.esprit.planning.repository.ProgressUpdateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health and readiness endpoint for the Planning microservice. Exposes /api/planning/health with service status and a lightweight database check.
 */
@RestController
@RequestMapping("/api/planning")
@CrossOrigin(origins = "*")
@Tag(name = "Planning Health", description = "Health and readiness checks for the Planning microservice")
public class PlanningHealthController {

    private final ProgressUpdateRepository progressUpdateRepository;

    public PlanningHealthController(ProgressUpdateRepository progressUpdateRepository) {
        this.progressUpdateRepository = progressUpdateRepository;
    }

    /** Returns service health: status UP with database count when DB is reachable; 503 with status DEGRADED when DB fails. */
    @GetMapping("/health")
    @Operation(
            summary = "Planning health",
            description = "Simple health/readiness endpoint for the Planning microservice, including a lightweight database check."
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy", content = @Content(schema = @Schema(implementation = Map.class)))
    // Performs health.
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("service", "planning");
        body.put("timestamp", Instant.now().toString());

        try {
            long count = progressUpdateRepository.count();
            Map<String, Object> db = new HashMap<>();
            db.put("status", "UP");
            db.put("progressUpdateCount", count);
            body.put("database", db);
            body.put("status", "UP");
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            Map<String, Object> db = new HashMap<>();
            db.put("status", "DOWN");
            db.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            body.put("database", db);
            body.put("status", "DEGRADED");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}

