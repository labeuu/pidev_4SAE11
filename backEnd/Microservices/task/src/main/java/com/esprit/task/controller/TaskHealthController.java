package com.esprit.task.controller;

import com.esprit.task.repository.TaskRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Task Health", description = "Health and readiness checks")
public class TaskHealthController {

    private final TaskRepository taskRepository;

    @GetMapping("/health")
    @Operation(summary = "Task health", description = "Health endpoint with database check.")
    @ApiResponse(responseCode = "200", description = "Service is healthy", content = @Content(schema = @Schema(implementation = Map.class)))
    // Performs health.
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("service", "task");
        body.put("timestamp", Instant.now().toString());

        try {
            long count = taskRepository.count();
            Map<String, Object> db = new HashMap<>();
            db.put("status", "UP");
            db.put("taskCount", count);
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
