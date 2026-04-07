package org.example.vendor.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewRemoteDto {
    private Long id;
    private Long reviewerId;
    private Long revieweeId;
    private Long projectId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
