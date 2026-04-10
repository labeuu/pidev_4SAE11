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
@Schema(description = "Parsed definition-of-done checklist from AI")
public class TaskAiDefinitionOfDoneResponse {

    private List<TaskAiDefinitionOfDoneCriterionDto> criteria;
    private List<String> assumptions;
}
