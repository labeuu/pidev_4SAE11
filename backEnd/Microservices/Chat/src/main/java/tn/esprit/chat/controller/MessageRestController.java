package tn.esprit.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.chat.dto.ChatMessageDTO;
import tn.esprit.chat.dto.ConversationSummary;
import tn.esprit.chat.service.IChatService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MessageRestController {

    private final IChatService chatService;

    @GetMapping("/conversation/{user1}/{user2}")
    public ResponseEntity<Page<ChatMessageDTO>> getConversation(
            @PathVariable Long user1,
            @PathVariable Long user2,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getConversation(user1, user2, page, size));
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<ChatMessageDTO>> getUnreadMessages(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getUnreadMessages(userId));
    }

    @GetMapping("/unread/count/{userId}")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getUnreadCount(userId));
    }

    @PutMapping("/seen/{messageId}")
    public ResponseEntity<ChatMessageDTO> markAsSeen(@PathVariable Long messageId) {
        return ResponseEntity.ok(chatService.markAsSeen(messageId));
    }

    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<ConversationSummary>> getConversations(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getConversations(userId));
    }
}
