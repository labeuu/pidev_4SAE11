package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Narrative workload coaching from the LLM (markdown-safe plain text sections). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workload coach response")
public class TaskAiWorkloadCoachResponse {

    @Schema(description = "Main coaching summary (plain paragraphs)")
    private String summaryMarkdown;

    @Schema(description = "Bullet highlights parsed or derived from the model output")
    private List<String> highlights;
}
