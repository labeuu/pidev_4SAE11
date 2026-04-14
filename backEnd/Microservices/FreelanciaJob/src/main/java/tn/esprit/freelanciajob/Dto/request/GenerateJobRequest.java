package tn.esprit.freelanciajob.Dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateJobRequest {

    @NotBlank(message = "prompt is required")
    private String prompt;
}
