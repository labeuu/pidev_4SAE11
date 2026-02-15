package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferStatsResponse {

    private Long totalOffers;
    private Long activeOffers;
    private Long draftOffers;
    private Long acceptedOffers;
    private Long expiredOffers;
    private Long totalApplications;
    private Long pendingApplications;
    private BigDecimal averageRating;
    private BigDecimal totalRevenue;
    private Integer totalViews;
    private Long offersThisMonth;
    private Long offersThisWeek;
}