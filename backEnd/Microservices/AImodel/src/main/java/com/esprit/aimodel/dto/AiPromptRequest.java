package com.esprit.aimodel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptRequest {

    @NotBlank(message = "prompt is required")
    private String prompt;

    /**
     * Optional Ollama {@code num_predict} cap for faster short answers (coach / Q&amp;A). When null, the model default applies.
     * Typical fast range: 256–768.
     */
    private Integer maxOutputTokens;
}
