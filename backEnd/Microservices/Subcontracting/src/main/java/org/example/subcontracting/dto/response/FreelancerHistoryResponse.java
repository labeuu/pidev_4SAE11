package org.example.subcontracting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerHistoryResponse {
    private Long userId;
    private String userName;
    private long totalEvents;
    private Map<String, Long> eventsByAction;
    private long asMainFreelancer;
    private long asSubcontractor;
    private List<AuditTimelineEntry> timeline;
}
