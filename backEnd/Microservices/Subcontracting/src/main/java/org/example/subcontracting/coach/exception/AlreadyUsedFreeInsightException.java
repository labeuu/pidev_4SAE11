package org.example.subcontracting.coach.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AlreadyUsedFreeInsightException extends ResponseStatusException {
    public AlreadyUsedFreeInsightException() {
        super(HttpStatus.CONFLICT, "L'analyse gratuite a déjà été utilisée pour ce compte.");
    }
}
