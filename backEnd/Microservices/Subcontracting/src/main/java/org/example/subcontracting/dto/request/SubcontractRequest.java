package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SubcontractRequest {

    @NotNull(message = "Subcontractor ID is required")
    private Long subcontractorId;

    /** Soit {@code projectId} (mission Project), soit {@code offerId} (mission Offer) — exactement un des deux. */
    private Long projectId;

    private Long offerId;

    private Long contractId;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Scope must not exceed 5000 characters")
    private String scope;

    @NotBlank(message = "Category is required")
    @Size(max = 64)
    private String category;

    @Positive(message = "Budget must be positive when provided")
    private BigDecimal budget;

    private String currency;

    private LocalDate startDate;

    private LocalDate deadline;

    /** Compétences requises (affichage e-mail + suivi métier) */
    private List<String> requiredSkills;

    /** URL du média après upload (voir POST /api/subcontracts/media/upload) */
    private String mediaUrl;

    /** VIDEO ou AUDIO — doit correspondre au fichier uploadé */
    private String mediaType;
}
