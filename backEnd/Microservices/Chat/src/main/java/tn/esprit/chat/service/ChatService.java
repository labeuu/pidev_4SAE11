package tn.esprit.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.chat.dto.ChatMessageDTO;
import tn.esprit.chat.dto.ConversationSummary;
import tn.esprit.chat.dto.SendMessageRequest;
import tn.esprit.chat.entity.ChatMessage;
import tn.esprit.chat.entity.enums.MessageStatus;
import tn.esprit.chat.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService implements IChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserStatusService userStatusService;

    @Override
    public ChatMessageDTO sendMessage(SendMessageRequest request) {
        ChatMessage message = ChatMessage.builder()
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .content(request.getContent())
                .timestamp(LocalDateTime.now())
                .status(MessageStatus.SENT)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.debug("Message saved with id={} from {} to {}", saved.getId(), saved.getSenderId(), saved.getReceiverId());
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDTO> getConversation(Long user1, Long user2, int page, int size) {
        return chatMessageRepository
                .findConversation(user1, user2, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getUnreadMessages(Long userId) {
        return chatMessageRepository
                .findByReceiverIdAndStatusNot(userId, MessageStatus.SEEN)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCount(Long userId) {
        List<ChatMessage> unread = chatMessageRepository.findByReceiverIdAndStatusNot(userId, MessageStatus.SEEN);

        Map<String, Long> result = new HashMap<>();
        result.put("total", (long) unread.size());

        Map<Long, Long> perSender = unread.stream()
                .collect(Collectors.groupingBy(ChatMessage::getSenderId, Collectors.counting()));

        perSender.forEach((senderId, count) -> result.put("sender_" + senderId, count));

        return result;
    }

    @Override
    public ChatMessageDTO markAsSeen(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));
        message.setStatus(MessageStatus.SEEN);
        ChatMessage saved = chatMessageRepository.save(message);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummary> getConversations(Long userId) {
        List<ChatMessage> latestMessages = chatMessageRepository.findLatestMessagesPerConversation(userId);

        return latestMessages.stream().map(msg -> {
            Long partnerId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            long unreadCount = chatMessageRepository.countByReceiverIdAndSenderIdAndStatusNot(
                    userId, partnerId, MessageStatus.SEEN);

            return ConversationSummary.builder()
                    .partnerId(partnerId)
                    .lastMessage(msg.getContent())
                    .lastMessageTime(msg.getTimestamp())
                    .lastMessageStatus(msg.getStatus().name())
                    .unreadCount(unreadCount)
                    .isOnline(userStatusService.isOnline(partnerId))
                    .build();
        }).collect(Collectors.toList());
    }

    private ChatMessageDTO toDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .status(message.getStatus().name())
                .build();
    }
}
