package org.example.offer.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferRequest {

    @NotNull(message = "Freelancer ID is required")
    private Long freelancerId;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 255)
    private String title;

    @NotBlank(message = "Domain is required")
    private String domain;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @NotBlank(message = "Duration type is required")
    @Pattern(regexp = "hourly|fixed|monthly")
    private String durationType;

    private LocalDate deadline;

    private String category;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private BigDecimal rating;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private BigDecimal communicationScore;

    private String tags;

    private String imageUrl;

    private Boolean isFeatured;

    // ✅ Utiliser l'ID au lieu de l'objet complet
    private Long projectStatusId;
}