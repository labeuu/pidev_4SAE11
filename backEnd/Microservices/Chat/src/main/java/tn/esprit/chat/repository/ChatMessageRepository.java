package tn.esprit.chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.chat.entity.ChatMessage;
import tn.esprit.chat.entity.enums.MessageStatus;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :user1 AND m.receiverId = :user2) OR (m.senderId = :user2 AND m.receiverId = :user1) ORDER BY m.timestamp ASC")
    Page<ChatMessage> findConversation(@Param("user1") Long user1, @Param("user2") Long user2, Pageable pageable);

    List<ChatMessage> findByReceiverIdAndStatusNot(Long receiverId, MessageStatus status);

    long countByReceiverIdAndSenderIdAndStatusNot(Long receiverId, Long senderId, MessageStatus status);

    @Query("SELECT DISTINCT CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END FROM ChatMessage m WHERE m.senderId = :userId OR m.receiverId = :userId")
    List<Long> findConversationPartners(@Param("userId") Long userId);

    @Query("SELECT m FROM ChatMessage m WHERE m.id IN (SELECT MAX(m2.id) FROM ChatMessage m2 WHERE (m2.senderId = :userId OR m2.receiverId = :userId) GROUP BY CASE WHEN m2.senderId = :userId THEN m2.receiverId ELSE m2.senderId END) ORDER BY m.timestamp DESC")
    List<ChatMessage> findLatestMessagesPerConversation(@Param("userId") Long userId);
}
