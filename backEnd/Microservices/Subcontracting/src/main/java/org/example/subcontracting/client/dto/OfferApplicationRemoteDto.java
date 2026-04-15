package org.example.subcontracting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Sous-ensemble des champs renvoyés par Offer (GET /api/applications/...).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfferApplicationRemoteDto {
    private Long offerId;
    private String offerTitle;
}
