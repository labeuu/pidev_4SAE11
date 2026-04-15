package com.esprit.planning.config;

import com.esprit.planning.exception.EntityNotFoundException;
import com.esprit.planning.exception.ProgressCannotDecreaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler. Returns standardized error envelope:
 * { "error": { "code": "...", "message": "...", "details": [...] } }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    // Handles entity not found.
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse("NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(ProgressCannotDecreaseException.class)
    // Handles progress cannot decrease.
    public ResponseEntity<Map<String, Object>> handleProgressCannotDecrease(ProgressCannotDecreaseException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", "VALIDATION_ERROR");
        error.put("message", ex.getMessage());
        error.put("details", List.of(Map.of("minAllowed", ex.getMinAllowed(), "provided", ex.getProvided())));
        error.put("minAllowed", ex.getMinAllowed());
        error.put("provided", ex.getProvided());
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Handles validation.
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("VALIDATION_ERROR", "Invalid request", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    // Handles illegal argument.
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorResponse("BAD_REQUEST", ex.getMessage(), null));
    }

    // Performs error response.
    private static Map<String, Object> errorResponse(String code, String message, Object details) {
        return Map.of(
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "details", details != null ? details : List.of()
                )
        );
    }
}
