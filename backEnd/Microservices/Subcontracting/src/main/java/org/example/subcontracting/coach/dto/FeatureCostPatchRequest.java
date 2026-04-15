package org.example.subcontracting.coach.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeatureCostPatchRequest {
    @NotNull
    @Min(0)
    private Integer costPoints;
}
