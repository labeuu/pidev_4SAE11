package com.esprit.aimodel.service;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final LlmGenerationService llmGenerationService;

    public AiService(LlmGenerationService llmGenerationService) {
        this.llmGenerationService = llmGenerationService;
    }

    public String generateResponse(String prompt) {
        return llmGenerationService.generate(prompt);
    }

    public String generateTasks(String context) {
        String prompt = TextSanitizer.taskPlannerPromptPrefix() + context;
        return TextSanitizer.stripModelNoise(llmGenerationService.generate(prompt));
    }

    public String generateSubtasks(String context) {
        String prompt = TextSanitizer.subtaskPlannerPromptPrefix() + context;
        return TextSanitizer.stripModelNoise(llmGenerationService.generate(prompt));
    }
}
