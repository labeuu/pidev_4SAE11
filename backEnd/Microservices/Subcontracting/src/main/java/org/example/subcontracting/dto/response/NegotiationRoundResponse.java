package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationRoundResponse {
    private OfferPosition primaryOffer;
    private OfferPosition subcontractorOffer;
    private OfferPosition aiCompromise;
    private String compromiseJustification;
    private Integer roundNumber;
    private String negotiationStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferPosition {
        private BigDecimal budget;
        private Integer durationDays;
    }
}

