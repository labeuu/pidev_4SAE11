package org.example.vendor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Pré-remplissage pour le renouvellement semi-automatique d'un agrément.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorRenewalPreviewResponse {

    private Long vendorApprovalId;
    private Long organizationId;
    private Long freelancerId;
    private String domain;
    private String professionalSector;
    private Integer currentReviewCount;
    private LocalDate previousValidFrom;
    private LocalDate previousValidUntil;
    private LocalDate suggestedValidFrom;
    private LocalDate suggestedValidUntil;
    private LocalDate suggestedNextReviewDate;
    private String previousApprovalNotes;
    private boolean canRenew;
    private String cannotRenewReason;
}
