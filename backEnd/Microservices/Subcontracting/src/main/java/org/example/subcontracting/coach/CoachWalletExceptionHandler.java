package org.example.subcontracting.coach;

import org.example.subcontracting.coach.exception.InsufficientPointsException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "org.example.subcontracting")
public class CoachWalletExceptionHandler {

    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<Map<String, Object>> insufficientPoints(InsufficientPointsException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currentBalance", ex.getCurrentBalance());
        body.put("requiredPoints", ex.getRequiredPoints());
        body.put("shortage", ex.getShortage());
        body.put("message", "Solde insuffisant");
        return ResponseEntity.badRequest().body(body);
    }
}
