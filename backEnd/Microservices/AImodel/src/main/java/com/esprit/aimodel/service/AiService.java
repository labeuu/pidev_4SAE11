package com.esprit.aimodel.service;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final LlmGenerationService llmGenerationService;

    public AiService(LlmGenerationService llmGenerationService) {
        this.llmGenerationService = llmGenerationService;
    }

    public String generateResponse(String prompt) {
        return llmGenerationService.generate(prompt, null);
    }

    /**
     * @param maxOutputTokens when set, maps to Ollama {@code num_predict} for shorter / faster generations
     */
    public String generateResponse(String prompt, Integer maxOutputTokens) {
        return llmGenerationService.generate(prompt, maxOutputTokens);
    }

    public String generateTasks(String context) {
        String prompt = TextSanitizer.taskPlannerPromptPrefix() + context;
        return TextSanitizer.stripModelNoise(llmGenerationService.generate(prompt, null));
    }

    public String generateSubtasks(String context) {
        String prompt = TextSanitizer.subtaskPlannerPromptPrefix() + context;
        return TextSanitizer.stripModelNoise(llmGenerationService.generate(prompt, null));
    }
}
