package com.esprit.task.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptRequest {
    private String prompt;
    /** Pass-through to AImodel / Ollama {@code num_predict} for faster short outputs (coach, ask-tasks). */
    private Integer maxOutputTokens;
}
