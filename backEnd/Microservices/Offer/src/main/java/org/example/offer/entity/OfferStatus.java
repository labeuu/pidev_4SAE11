package org.example.offer.entity;

public enum OfferStatus {
    DRAFT,       // Brouillon (pas encore publié)
    AVAILABLE,   // Disponible pour candidatures
    IN_PROGRESS, // En cours (candidat sélectionné)
    ACCEPTED,    // Acceptée (contrat signé)
    COMPLETED,   // Terminée
    CANCELLED,   // Annulée
    EXPIRED,     // Expirée
    CLOSED       // Fermée
}