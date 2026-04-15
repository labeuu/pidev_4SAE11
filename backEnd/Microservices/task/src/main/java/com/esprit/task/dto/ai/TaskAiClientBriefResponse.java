package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generated brief plus optional warning when Planning Feign failed (task-only degradation).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI stakeholder brief response")
public class TaskAiClientBriefResponse {

    @Schema(description = "Brief for email/chat; human must review before sending")
    private String briefMarkdown;

    /**
     * Non-null when Planning could not be reached — brief still uses task-board data only.
     */
    @Schema(description = "Set when progress-update context was omitted due to upstream failure")
    private String planningDataWarning;
}
