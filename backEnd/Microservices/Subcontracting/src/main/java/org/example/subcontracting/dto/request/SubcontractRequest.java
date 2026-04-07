package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubcontractRequest {

    @NotNull(message = "Subcontractor ID is required")
    private Long subcontractorId;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private Long contractId;

    @NotBlank(message = "Title is required")
    private String title;

    private String scope;

    @NotNull(message = "Category is required")
    private String category;

    @Positive(message = "Budget must be positive")
    private BigDecimal budget;

    private String currency;

    private LocalDate startDate;

    private LocalDate deadline;
}
