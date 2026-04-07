package org.example.vendor.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorApprovalRequest {

    @NotNull(message = "Organization (client) ID is required")
    private Long organizationId;

    @NotNull(message = "Freelancer ID is required")
    private Long freelancerId;

    private String domain;

    /** MÉTIER 6 — Secteur métier (optionnel). */
    private String professionalSector;

    private String notes;
}
