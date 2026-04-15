package org.example.subcontracting.coach.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class WalletBlockedException extends ResponseStatusException {
    public WalletBlockedException() {
        super(HttpStatus.FORBIDDEN, "Votre accès coaching est suspendu. Contactez l'administrateur.");
    }
}
