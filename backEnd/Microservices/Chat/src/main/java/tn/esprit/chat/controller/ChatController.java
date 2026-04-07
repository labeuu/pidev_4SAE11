package tn.esprit.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import tn.esprit.chat.dto.ChatMessageDTO;
import tn.esprit.chat.dto.SendMessageRequest;
import tn.esprit.chat.dto.TypingEvent;
import tn.esprit.chat.service.IChatService;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final IChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat")
    public void handleMessage(SendMessageRequest request, Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        request.setSenderId(senderId);

        ChatMessageDTO saved = chatService.sendMessage(request);

        // Deliver to receiver
        messagingTemplate.convertAndSendToUser(
                request.getReceiverId().toString(),
                "/queue/messages",
                saved
        );

        // Echo back to sender
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/messages",
                saved
        );

        log.debug("Message routed: id={} from {} to {}", saved.getId(), senderId, request.getReceiverId());
    }

    @MessageMapping("/typing")
    public void handleTyping(TypingEvent event, Principal principal) {
        Long senderId = Long.parseLong(principal.getName());
        event.setSenderId(senderId);

        messagingTemplate.convertAndSendToUser(
                event.getReceiverId().toString(),
                "/queue/typing",
                event
        );
    }

    @MessageMapping("/seen")
    public void handleSeen(Map<String, Long> payload, Principal principal) {
        Long messageId = payload.get("messageId");
        if (messageId == null) {
            log.warn("Received /seen without messageId in payload");
            return;
        }

        ChatMessageDTO updated = chatService.markAsSeen(messageId);

        // Notify the original sender that their message was seen
        messagingTemplate.convertAndSendToUser(
                updated.getSenderId().toString(),
                "/queue/seen",
                updated
        );
    }
}
