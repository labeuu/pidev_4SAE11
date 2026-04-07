package org.example.vendor.client.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ReviewStatsRemoteDto {
    private long totalCount;
    private double averageRating;
    private Map<Integer, Long> countByRating;
}
