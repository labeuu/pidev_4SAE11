package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Taux d'acceptation des candidatures (calcul backend). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptanceRateResponse {
    private Long totalApplications;
    private Long acceptedCount;
    private BigDecimal rate; // 0.0 à 1.0
}
