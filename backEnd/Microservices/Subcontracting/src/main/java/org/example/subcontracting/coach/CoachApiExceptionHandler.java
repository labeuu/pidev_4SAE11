package org.example.subcontracting.coach;

import lombok.extern.slf4j.Slf4j;
import org.example.subcontracting.coach.controller.CoachFeatureCostController;
import org.example.subcontracting.coach.controller.CoachInsightController;
import org.example.subcontracting.coach.controller.CoachWalletController;
import org.example.subcontracting.coach.exception.AlreadyUsedFreeInsightException;
import org.example.subcontracting.coach.exception.WalletBlockedException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erreurs API coach : messages explicites (évite le corps Spring générique
 * {@code "No message available"} quand l’exception n’a pas de détail).
 */
@RestControllerAdvice(assignableTypes = {
        CoachInsightController.class,
        CoachWalletController.class,
        CoachFeatureCostController.class
})
@Order(1)
@Slf4j
public class CoachApiExceptionHandler {

    /** Ne pas intercepter {@code InsufficientPointsException} : gérée par {@code CoachWalletExceptionHandler}. */

    @ExceptionHandler(AlreadyUsedFreeInsightException.class)
    public ResponseEntity<Map<String, Object>> alreadyUsedFree(AlreadyUsedFreeInsightException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "message", reasonOrDefault(ex, "L'analyse gratuite a déjà été utilisée.")));
    }

    @ExceptionHandler(WalletBlockedException.class)
    public ResponseEntity<Map<String, Object>> walletBlocked(WalletBlockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "message", reasonOrDefault(ex, "Accès coaching suspendu.")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> dataIntegrity(DataIntegrityViolationException ex) {
        log.error("[COACH-API] Contrainte base de données", ex);
        String root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Données refusées par la base (coach). Vérifiez les identifiants ou réessayez.",
                "detail", root != null ? root : ""));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> dataAccess(DataAccessException ex) {
        log.error("[COACH-API] Erreur d’accès aux données", ex);
        String root = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Erreur base de données pendant l’opération coaching.",
                "detail", root != null ? root : ""));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> fallback(Exception ex) {
        log.error("[COACH-API] Erreur non gérée", ex);
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank() || "No message available".equalsIgnoreCase(msg.trim())) {
            msg = "Erreur technique sur le service coaching. Consultez les logs Subcontracting.";
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", msg);
        body.put("type", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private static String reasonOrDefault(ResponseStatusException ex, String fallback) {
        String r = ex.getReason();
        if (r == null || r.isBlank() || "No message available".equalsIgnoreCase(r.trim())) {
            return fallback;
        }
        return r;
    }
}
