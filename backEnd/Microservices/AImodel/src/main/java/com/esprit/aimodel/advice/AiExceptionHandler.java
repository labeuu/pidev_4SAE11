package com.esprit.aimodel.advice;

import com.esprit.aimodel.dto.AiErrorEnvelope;
import com.esprit.aimodel.exception.AiUpstreamException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AiErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "Validation error")
                .orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AiErrorEnvelope.of(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AiErrorEnvelope> handleBadJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AiErrorEnvelope.of("Invalid JSON body"));
    }

    @ExceptionHandler(AiUpstreamException.class)
    public ResponseEntity<AiErrorEnvelope> handleUpstream(AiUpstreamException ex) {
        return ResponseEntity.status(ex.getStatus()).body(AiErrorEnvelope.of(ex.getMessage()));
    }
}
