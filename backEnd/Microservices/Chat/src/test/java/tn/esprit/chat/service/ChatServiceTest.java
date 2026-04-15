package tn.esprit.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tn.esprit.chat.dto.ChatMessageDTO;
import tn.esprit.chat.dto.ConversationSummary;
import tn.esprit.chat.dto.SendMessageRequest;
import tn.esprit.chat.entity.ChatMessage;
import tn.esprit.chat.entity.enums.MessageStatus;
import tn.esprit.chat.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ChatService}.
 *
 * All repository and status-service calls are mocked. Every test follows the
 * Arrange / Act / Assert (AAA) pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService – Unit Tests")
class ChatServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserStatusService     userStatusService;

    @InjectMocks
    private ChatService chatService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private static final Long SENDER_ID   = 1L;
    private static final Long RECEIVER_ID = 2L;

    private ChatMessage buildMessage(Long id, Long senderId, Long receiverId,
                                     String content, MessageStatus status) {
        ChatMessage m = new ChatMessage();
        m.setId(id);
        m.setSenderId(senderId);
        m.setReceiverId(receiverId);
        m.setContent(content);
        m.setTimestamp(LocalDateTime.now());
        m.setStatus(status);
        return m;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // sendMessage()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendMessage()")
    class SendMessageTests {

        @Test
        @DisplayName("should persist message with SENT status and return DTO")
        void validRequest_persistsAndReturnsDto() {
            // Arrange
            SendMessageRequest req = new SendMessageRequest(SENDER_ID, RECEIVER_ID, "Hello!");
            ChatMessage saved = buildMessage(1L, SENDER_ID, RECEIVER_ID, "Hello!", MessageStatus.SENT);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(saved);

            // Act
            ChatMessageDTO result = chatService.sendMessage(req);

            // Assert
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSenderId()).isEqualTo(SENDER_ID);
            assertThat(result.getReceiverId()).isEqualTo(RECEIVER_ID);
            assertThat(result.getContent()).isEqualTo("Hello!");
            assertThat(result.getStatus()).isEqualTo("SENT");
            verify(chatMessageRepository).save(argThat(m ->
                    m.getStatus() == MessageStatus.SENT
                    && m.getSenderId().equals(SENDER_ID)
                    && m.getReceiverId().equals(RECEIVER_ID)
            ));
        }

        @Test
        @DisplayName("should set timestamp on the persisted message")
        void validRequest_setsTimestamp() {
            // Arrange
            SendMessageRequest req = new SendMessageRequest(SENDER_ID, RECEIVER_ID, "Hi");
            ChatMessage saved = buildMessage(1L, SENDER_ID, RECEIVER_ID, "Hi", MessageStatus.SENT);
            when(chatMessageRepository.save(any())).thenReturn(saved);

            // Act
            ChatMessageDTO result = chatService.sendMessage(req);

            // Assert
            assertThat(result.getTimestamp()).isNotNull();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getConversation()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getConversation()")
    class GetConversationTests {

        @Test
        @DisplayName("should return paginated messages between two users")
        void twoUsers_returnsPaginatedMessages() {
            // Arrange
            List<ChatMessage> msgs = List.of(
                    buildMessage(1L, SENDER_ID, RECEIVER_ID, "Hi",       MessageStatus.SEEN),
                    buildMessage(2L, RECEIVER_ID, SENDER_ID, "Hey back", MessageStatus.SENT)
            );
            Page<ChatMessage> page = new PageImpl<>(msgs);
            when(chatMessageRepository.findConversation(eq(SENDER_ID), eq(RECEIVER_ID), any(PageRequest.class)))
                    .thenReturn(page);

            // Act
            Page<ChatMessageDTO> result = chatService.getConversation(SENDER_ID, RECEIVER_ID, 0, 20);

            // Assert
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("Hi");
        }

        @Test
        @DisplayName("should return empty page when conversation does not exist")
        void noMessages_returnsEmptyPage() {
            // Arrange
            when(chatMessageRepository.findConversation(any(), any(), any()))
                    .thenReturn(Page.empty());

            // Act
            Page<ChatMessageDTO> result = chatService.getConversation(SENDER_ID, RECEIVER_ID, 0, 20);

            // Assert
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getUnreadMessages()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUnreadMessages()")
    class GetUnreadMessagesTests {

        @Test
        @DisplayName("should return all messages where status is not SEEN")
        void withUnreadMessages_returnsCorrectList() {
            // Arrange
            List<ChatMessage> unread = List.of(
                    buildMessage(1L, SENDER_ID, RECEIVER_ID, "Read me", MessageStatus.SENT),
                    buildMessage(2L, SENDER_ID, RECEIVER_ID, "Me too",  MessageStatus.DELIVERED)
            );
            when(chatMessageRepository.findByReceiverIdAndStatusNot(RECEIVER_ID, MessageStatus.SEEN))
                    .thenReturn(unread);

            // Act
            List<ChatMessageDTO> result = chatService.getUnreadMessages(RECEIVER_ID);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).noneMatch(dto -> "SEEN".equals(dto.getStatus()));
        }

        @Test
        @DisplayName("should return empty list when all messages are SEEN")
        void allSeen_returnsEmptyList() {
            // Arrange
            when(chatMessageRepository.findByReceiverIdAndStatusNot(RECEIVER_ID, MessageStatus.SEEN))
                    .thenReturn(List.of());

            // Act
            List<ChatMessageDTO> result = chatService.getUnreadMessages(RECEIVER_ID);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getUnreadCount()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCountTests {

        @Test
        @DisplayName("should return total count and per-sender breakdown")
        void withUnread_returnsTotalAndPerSenderMap() {
            // Arrange – 2 unread from sender 1, 1 unread from sender 3
            List<ChatMessage> unread = List.of(
                    buildMessage(1L, 1L, RECEIVER_ID, "A", MessageStatus.SENT),
                    buildMessage(2L, 1L, RECEIVER_ID, "B", MessageStatus.DELIVERED),
                    buildMessage(3L, 3L, RECEIVER_ID, "C", MessageStatus.SENT)
            );
            when(chatMessageRepository.findByReceiverIdAndStatusNot(RECEIVER_ID, MessageStatus.SEEN))
                    .thenReturn(unread);

            // Act
            Map<String, Long> result = chatService.getUnreadCount(RECEIVER_ID);

            // Assert
            assertThat(result.get("total")).isEqualTo(3L);
            assertThat(result.get("sender_1")).isEqualTo(2L);
            assertThat(result.get("sender_3")).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return total=0 when no unread messages exist")
        void noUnread_returnsTotalZero() {
            // Arrange
            when(chatMessageRepository.findByReceiverIdAndStatusNot(RECEIVER_ID, MessageStatus.SEEN))
                    .thenReturn(List.of());

            // Act
            Map<String, Long> result = chatService.getUnreadCount(RECEIVER_ID);

            // Assert
            assertThat(result.get("total")).isZero();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // markAsSeen()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("markAsSeen()")
    class MarkAsSeenTests {

        @Test
        @DisplayName("should update message status to SEEN and return updated DTO")
        void existingMessage_updatesStatusToSeen() {
            // Arrange
            ChatMessage existing = buildMessage(1L, SENDER_ID, RECEIVER_ID, "Hi!", MessageStatus.SENT);
            when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(chatMessageRepository.save(any())).thenReturn(existing);

            // Act
            ChatMessageDTO result = chatService.markAsSeen(1L);

            // Assert
            verify(chatMessageRepository).save(argThat(m -> m.getStatus() == MessageStatus.SEEN));
            assertThat(result.getStatus()).isEqualTo("SEEN");
        }

        @Test
        @DisplayName("should throw RuntimeException when message does not exist")
        void unknownMessageId_throwsRuntimeException() {
            // Arrange
            when(chatMessageRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> chatService.markAsSeen(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("999");

            verify(chatMessageRepository, never()).save(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getConversations()
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getConversations()")
    class GetConversationsTests {

        @Test
        @DisplayName("should return one ConversationSummary per unique conversation partner")
        void withMessages_returnsConversationSummaries() {
            // Arrange – latest message from user 1 ↔ user 2
            ChatMessage latestMsg = buildMessage(5L, SENDER_ID, RECEIVER_ID, "Latest!", MessageStatus.SENT);
            when(chatMessageRepository.findLatestMessagesPerConversation(RECEIVER_ID))
                    .thenReturn(List.of(latestMsg));
            when(chatMessageRepository.countByReceiverIdAndSenderIdAndStatusNot(
                    RECEIVER_ID, SENDER_ID, MessageStatus.SEEN)).thenReturn(2L);
            when(userStatusService.isOnline(SENDER_ID)).thenReturn(true);

            // Act
            List<ConversationSummary> result = chatService.getConversations(RECEIVER_ID);

            // Assert
            assertThat(result).hasSize(1);
            ConversationSummary summary = result.get(0);
            assertThat(summary.getPartnerId()).isEqualTo(SENDER_ID);
            assertThat(summary.getLastMessage()).isEqualTo("Latest!");
            assertThat(summary.getUnreadCount()).isEqualTo(2L);
            assertThat(summary.isOnline()).isTrue();
        }

        @Test
        @DisplayName("should return empty list when user has no messages")
        void noMessages_returnsEmptyList() {
            // Arrange
            when(chatMessageRepository.findLatestMessagesPerConversation(RECEIVER_ID))
                    .thenReturn(List.of());

            // Act
            List<ConversationSummary> result = chatService.getConversations(RECEIVER_ID);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should correctly identify partner: when user is sender, partner is receiver")
        void userIsSender_partnerIsReceiver() {
            // Arrange – SENDER_ID sent the last message; we query for SENDER_ID's conversations
            ChatMessage latestMsg = buildMessage(5L, SENDER_ID, RECEIVER_ID, "Hi!", MessageStatus.SENT);
            when(chatMessageRepository.findLatestMessagesPerConversation(SENDER_ID))
                    .thenReturn(List.of(latestMsg));
            when(chatMessageRepository.countByReceiverIdAndSenderIdAndStatusNot(any(), any(), any()))
                    .thenReturn(0L);
            when(userStatusService.isOnline(RECEIVER_ID)).thenReturn(false);

            // Act
            List<ConversationSummary> result = chatService.getConversations(SENDER_ID);

            // Assert – partner should be RECEIVER_ID (the other party)
            assertThat(result.get(0).getPartnerId()).isEqualTo(RECEIVER_ID);
        }
    }
}
