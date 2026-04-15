package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubcontractMessageResponse {
    private Long id;
    private Long subcontractId;
    private Long senderUserId;
    private String senderName;
    private String message;
    private LocalDateTime createdAt;
}
