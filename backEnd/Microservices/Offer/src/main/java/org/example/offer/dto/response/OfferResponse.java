package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.offer.entity.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponse {

    private Long id;
    private Long freelancerId;
    private String title;
    private String domain;
    private String description;
    private BigDecimal price;
    private String durationType;
    private OfferStatus offerStatus;
    private LocalDate deadline;
    private String category;
    private BigDecimal rating;
    private BigDecimal communicationScore;
    private String tags;
    private String imageUrl;
    private Integer viewsCount;
    private Boolean isFeatured;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime expiredAt;

    // Informations supplémentaires
    private Integer applicationsCount;
    private Long pendingApplicationsCount;

    /** ID du statut projet (optionnel, référence externe au microservice Project) */
    private Long projectStatusId;

    private Boolean canReceiveApplications;
    private Boolean isValid;
}