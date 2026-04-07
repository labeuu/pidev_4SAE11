package tn.esprit.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userIdHeader = accessor.getFirstNativeHeader("X-User-Id");
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    Long userId = Long.parseLong(userIdHeader.trim());
                    UsernamePasswordAuthenticationToken principal =
                            new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
                    accessor.setUser(principal);
                    log.debug("WebSocket CONNECT authenticated for userId={}", userId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid X-User-Id header value: {}", userIdHeader);
                }
            } else {
                log.warn("WebSocket CONNECT received without X-User-Id header");
            }
        }

        return message;
    }
}
