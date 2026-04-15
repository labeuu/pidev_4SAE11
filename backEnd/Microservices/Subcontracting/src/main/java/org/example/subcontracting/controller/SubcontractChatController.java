package org.example.subcontracting.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.subcontracting.dto.request.SubcontractMessageRequest;
import org.example.subcontracting.dto.response.SubcontractMessageResponse;
import org.example.subcontracting.service.SubcontractChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subcontracts/{subcontractId}/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubcontractChatController {

    private final SubcontractChatService chatService;

    @GetMapping
    public ResponseEntity<List<SubcontractMessageResponse>> getMessages(
            @PathVariable Long subcontractId,
            @RequestParam Long viewerUserId) {
        return ResponseEntity.ok(chatService.getMessages(subcontractId, viewerUserId));
    }

    @PostMapping
    public ResponseEntity<SubcontractMessageResponse> sendMessage(
            @PathVariable Long subcontractId,
            @RequestParam Long senderUserId,
            @Valid @RequestBody SubcontractMessageRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(subcontractId, senderUserId, body.getMessage()));
    }
}
