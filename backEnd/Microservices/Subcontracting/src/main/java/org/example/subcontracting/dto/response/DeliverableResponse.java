package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverableResponse {

    private Long id;
    private Long subcontractId;
    private String title;
    private String description;
    private String status;
    private LocalDate deadline;
    private String submissionUrl;
    private String submissionNote;
    private LocalDateTime submittedAt;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private boolean overdue;
}
