package com.esprit.aimodel.controller;

import com.esprit.aimodel.dto.AiContextRequest;
import com.esprit.aimodel.dto.AiGenerateResponse;
import com.esprit.aimodel.dto.AiLiveStatus;
import com.esprit.aimodel.dto.AiPromptRequest;
import com.esprit.aimodel.service.AiService;
import com.esprit.aimodel.service.ProviderStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final ProviderStatusService providerStatusService;

    public AiController(AiService aiService, ProviderStatusService providerStatusService) {
        this.aiService = aiService;
        this.providerStatusService = providerStatusService;
    }

    @GetMapping("/status")
    public ResponseEntity<AiLiveStatus> status() {
        return ResponseEntity.ok(providerStatusService.liveStatus());
    }

    @PostMapping("/generate")
    public ResponseEntity<AiGenerateResponse> generate(@Valid @RequestBody AiPromptRequest body) {
        String data = aiService.generateResponse(body.getPrompt().trim());
        return ResponseEntity.ok(new AiGenerateResponse(true, data));
    }

    @PostMapping("/generate-tasks")
    public ResponseEntity<AiGenerateResponse> generateTasks(@Valid @RequestBody AiContextRequest body) {
        String data = aiService.generateTasks(body.getContext().trim());
        return ResponseEntity.ok(new AiGenerateResponse(true, data));
    }

    @PostMapping("/generate-subtasks")
    public ResponseEntity<AiGenerateResponse> generateSubtasks(@Valid @RequestBody AiContextRequest body) {
        String data = aiService.generateSubtasks(body.getContext().trim());
        return ResponseEntity.ok(new AiGenerateResponse(true, data));
    }
}
