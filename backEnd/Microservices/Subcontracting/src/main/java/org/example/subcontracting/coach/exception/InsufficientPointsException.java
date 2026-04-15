package org.example.subcontracting.coach.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class InsufficientPointsException extends ResponseStatusException {
    private final int currentBalance;
    private final int requiredPoints;
    private final int shortage;

    public InsufficientPointsException(int currentBalance, int requiredPoints) {
        super(HttpStatus.BAD_REQUEST, "Solde insuffisant");
        this.currentBalance = currentBalance;
        this.requiredPoints = requiredPoints;
        this.shortage = Math.max(0, requiredPoints - currentBalance);
    }
}
