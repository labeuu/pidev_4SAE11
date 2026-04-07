package tn.esprit.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummary {

    private Long partnerId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String lastMessageStatus;
    private long unreadCount;
    private boolean isOnline;
}
