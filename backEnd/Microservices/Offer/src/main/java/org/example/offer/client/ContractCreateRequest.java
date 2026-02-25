package org.example.offer.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour la création d'un Contract via le microservice Contract.
 * Association par identifiants (offerId, clientId, freelancerId, offerApplicationId) sans dépendance JPA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractCreateRequest {

    private Long clientId;
    private Long freelancerId;
    private Long offerApplicationId;
    private Long projectApplicationId;
    private String title;
    private String description;
    private String terms;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
