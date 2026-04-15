package org.example.subcontracting.coach.dto;

import lombok.Data;

@Data
public class RechargeRequestDto {
    /** Montant de points souhaité (suggestion pour l’admin) */
    private Integer suggestedPoints;
    private String message;
}
