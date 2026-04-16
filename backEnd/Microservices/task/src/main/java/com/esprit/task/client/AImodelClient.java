package com.esprit.task.client;

import com.esprit.task.dto.ai.AiContextRequest;
import com.esprit.task.dto.ai.AiGenerateResponse;
import com.esprit.task.dto.ai.AiPromptRequest;
import com.esprit.task.config.AImodelFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "AIMODEL",
        path = "/api/ai",
        contextId = "aimodelAiClient",
        configuration = AImodelFeignConfiguration.class)
public interface AImodelClient {

    @PostMapping("/generate")
    AiGenerateResponse generate(@RequestBody AiPromptRequest body);

    @PostMapping("/generate-tasks")
    AiGenerateResponse generateTasks(@RequestBody AiContextRequest body);

    @PostMapping("/generate-subtasks")
    AiGenerateResponse generateSubtasks(@RequestBody AiContextRequest body);
}
