package org.example.offer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.offer.entity.OfferStatus;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferFilterRequest {

    private String keyword; // Recherche dans titre, description, tags
    private String domain;
    private String category;
    private OfferStatus offerStatus;
    private Long projectStatusId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minRating;
    private String durationType;
    private Boolean isFeatured;
    private Boolean isActive;
    private Long freelancerId;

    // Pagination
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}