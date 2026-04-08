package org.example.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MÉTIER 3 — Réponse détaillée pour intégration offres B2B / restreintes (motif si non éligible).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityDetailResponse {

    private boolean eligible;
    /** Ex. OK, NO_AGREEMENT, NOT_APPROVED, EXPIRED_DATE, DOMAIN_MISSING, DOMAIN_MISMATCH */
    private String reasonCode;
    private String message;

    public static EligibilityDetailResponse ok() {
        return new EligibilityDetailResponse(true, "OK", "Agrément actif et valide pour cette organisation.");
    }

    public static EligibilityDetailResponse no(String code, String message) {
        return new EligibilityDetailResponse(false, code, message);
    }
}
