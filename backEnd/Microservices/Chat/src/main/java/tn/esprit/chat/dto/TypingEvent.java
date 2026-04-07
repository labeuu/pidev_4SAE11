package tn.esprit.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingEvent {

    private Long senderId;
    private Long receiverId;
    private boolean typing;
}
