package com.esprit.task.dto.planning;

import lombok.Data;

/**
 * Minimal deserialization of Planning's create response (created entity contains at least {@code id}).
 */
@Data
public class PlanningProgressUpdateRefDto {

    private Long id;
}
