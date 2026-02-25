package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Statistiques : nombre d'offres par statut (calcul backend, agrégations). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OffersByStatusResponse {
    private Map<String, Long> countByStatus;
}
