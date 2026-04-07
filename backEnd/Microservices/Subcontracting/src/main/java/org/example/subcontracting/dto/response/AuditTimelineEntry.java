package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTimelineEntry {
    private Long id;
    private Long subcontractId;
    private String subcontractTitle;
    private String action;
    private String actionLabel;
    private String fromStatus;
    private String toStatus;
    private String detail;
    private String targetEntity;
    private Long targetEntityId;
    private Long actorUserId;
    private String actorName;
    private LocalDateTime createdAt;
    private String icon;
    private String color;
}
