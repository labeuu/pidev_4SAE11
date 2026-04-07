package com.esprit.ticket.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Long id) {
        super(entityName + " not found with id: " + id);
    }
}

