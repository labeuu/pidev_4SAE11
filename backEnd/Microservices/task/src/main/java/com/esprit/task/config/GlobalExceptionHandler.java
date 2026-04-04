package com.esprit.task.config;

import com.esprit.task.exception.EntityNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final ObjectMapper FEIGN_BODY_MAPPER = new ObjectMapper();

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "message", "Validation failed",
                "errors", fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Bad request";
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid or missing JSON body"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(Map.of("message", reason));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, String>> handleFeign(FeignException ex) {
        int s = ex.status();
        HttpStatus status;
        if (s == 408 || s == 504) {
            status = HttpStatus.GATEWAY_TIMEOUT;
        } else if (s == 503) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }
        String message = resolveFeignClientMessage(ex);
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    /**
     * Surfaces AImodel / Feign error bodies (e.g. {"error":{"message":"..."}}) and common transport hints.
     */
    static String resolveFeignClientMessage(FeignException ex) {
        String body = safeFeignBody(ex);
        String fromJson = extractMessageFromJsonBody(body);
        if (fromJson != null && !fromJson.isBlank()) {
            return fromJson.trim();
        }
        String fm = ex.getMessage();
        if (fm != null) {
            if (fm.contains("Connection refused")) {
                return "Cannot reach the AI model service. Start the AImodel microservice and check aimodel.service.url.";
            }
            if (fm.toLowerCase().contains("timeout") || fm.contains("timed out")) {
                return "The AI model service timed out. Ensure Ollama is running and timeouts are high enough.";
            }
        }
        if (body != null && !body.isBlank()) {
            String t = body.trim();
            return t.length() > 280 ? t.substring(0, 280) + "…" : t;
        }
        return "AI or upstream service error";
    }

    private static String safeFeignBody(FeignException ex) {
        try {
            return ex.contentUTF8();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extractMessageFromJsonBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = FEIGN_BODY_MAPPER.readTree(body);
            if (root.has("error") && root.get("error").isObject()) {
                JsonNode err = root.get("error");
                if (err.has("message") && err.get("message").isTextual()) {
                    return err.get("message").asText();
                }
            }
            if (root.has("message") && root.get("message").isTextual()) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
            // not JSON
        }
        return null;
    }
}
