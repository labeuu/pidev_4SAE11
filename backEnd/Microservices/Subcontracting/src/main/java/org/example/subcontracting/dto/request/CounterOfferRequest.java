package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounterOfferRequest {
    @DecimalMin(value = "0.01", message = "Le budget proposé doit être supérieur à 0")
    private BigDecimal proposedBudget;

    @Min(value = 1, message = "La durée proposée doit être d'au moins 1 jour")
    private Integer proposedDurationDays;

    private String note;
}

