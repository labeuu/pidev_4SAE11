package org.example.subcontracting.coach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCreditRequest {
    @NotNull
    @Min(1)
    private Integer amount;
    @NotBlank
    private String reason;
    private String adminNote;
}
