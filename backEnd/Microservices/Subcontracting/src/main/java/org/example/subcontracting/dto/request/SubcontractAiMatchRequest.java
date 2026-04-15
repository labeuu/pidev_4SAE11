package org.example.subcontracting.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubcontractAiMatchRequest {

    @NotEmpty(message = "Au moins une compétence requise")
    private List<String> requiredSkills;
}
