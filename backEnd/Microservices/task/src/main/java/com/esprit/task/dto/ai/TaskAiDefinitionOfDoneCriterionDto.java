package com.esprit.task.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One acceptance criterion from structured AI JSON. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAiDefinitionOfDoneCriterionDto {

    private String text;

    @Schema(description = "True when the criterion is mandatory for acceptance")
    private boolean mustHave;
}
