package org.example.vendor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Signature électronique : le signataire saisit son nom complet et son ID utilisateur doit correspondre
 * au client (organisation) ou au freelancer concerné par l'agrément.
 */
@Data
public class VendorSignatureRequest {

    @NotNull
    private Long signerUserId;

    @NotBlank
    private String fullName;
}
