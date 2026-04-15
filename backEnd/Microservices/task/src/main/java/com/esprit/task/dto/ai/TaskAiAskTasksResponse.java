package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Answer grounded in assignee task JSON; may cite task ids")
public class TaskAiAskTasksResponse {

    private String answerMarkdown;

    @Schema(description = "Root task ids mentioned as particularly relevant")
    private List<Long> citedTaskIds;
}
