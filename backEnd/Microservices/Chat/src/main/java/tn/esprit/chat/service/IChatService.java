package tn.esprit.chat.service;

import org.springframework.data.domain.Page;
import tn.esprit.chat.dto.ChatMessageDTO;
import tn.esprit.chat.dto.ConversationSummary;
import tn.esprit.chat.dto.SendMessageRequest;

import java.util.List;
import java.util.Map;

public interface IChatService {

    ChatMessageDTO sendMessage(SendMessageRequest request);

    Page<ChatMessageDTO> getConversation(Long user1, Long user2, int page, int size);

    List<ChatMessageDTO> getUnreadMessages(Long userId);

    Map<String, Long> getUnreadCount(Long userId);

    ChatMessageDTO markAsSeen(Long messageId);

    List<ConversationSummary> getConversations(Long userId);
}
