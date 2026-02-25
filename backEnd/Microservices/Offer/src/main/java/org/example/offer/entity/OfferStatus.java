package org.example.offer.entity;

/**
 * Cycle de vie type plateforme service-based (Fiverr) :
 * DRAFT → AVAILABLE → IN_PROGRESS → COMPLETED.
 * L'exécution (livraison, paiement) est gérée par le microservice Contract.
 */
public enum OfferStatus {
    DRAFT,       // Brouillon (pas encore publié)
    AVAILABLE,   // Disponible pour candidatures (catalogue)
    IN_PROGRESS, // En cours (candidat accepté, contrat créé)
    ACCEPTED,    // Acceptée (contrat signé) – aligné avec IN_PROGRESS
    COMPLETED,   // Terminée (synchronisé avec Contract COMPLETED)
    CANCELLED,   // Annulée
    EXPIRED,     // Expirée
    CLOSED       // Fermée
}