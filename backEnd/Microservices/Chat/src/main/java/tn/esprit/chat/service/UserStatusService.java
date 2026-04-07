package tn.esprit.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import tn.esprit.chat.dto.UserStatusEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatusService {

    private final SimpMessagingTemplate messagingTemplate;

    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        try {
            if (accessor.getUser() != null) {
                Long userId = Long.parseLong(accessor.getUser().getName());
                onlineUsers.add(userId);
                log.debug("User {} connected", userId);
                messagingTemplate.convertAndSend("/topic/user-status", new UserStatusEvent(userId, "ONLINE"));
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse userId from principal during connect: {}", e.getMessage());
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        try {
            if (accessor.getUser() != null) {
                Long userId = Long.parseLong(accessor.getUser().getName());
                onlineUsers.remove(userId);
                log.debug("User {} disconnected", userId);
                messagingTemplate.convertAndSend("/topic/user-status", new UserStatusEvent(userId, "OFFLINE"));
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse userId from principal during disconnect: {}", e.getMessage());
        }
    }

    public boolean isOnline(Long userId) {
        return onlineUsers.contains(userId);
    }

    public Set<Long> getOnlineUsers() {
        return Collections.unmodifiableSet(onlineUsers);
    }
}
