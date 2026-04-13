package com.esprit.notification.service;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Volatile store for local/dev when Firebase is disabled. Data is lost on restart.
 */
@Component
public class InMemoryNotificationStore {

    private static final String COLLECTION = "notifications";

    private final Map<String, Map<String, Object>> documents = new ConcurrentHashMap<>();

    public NotificationResponse create(NotificationRequest request) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", request.getUserId());
        data.put("title", request.getTitle());
        data.put("body", request.getBody() != null ? request.getBody() : "");
        data.put("type", request.getType() != null ? request.getType() : "GENERAL");
        data.put("read", false);
        data.put("createdAt", Instant.now().toString());
        if (request.getData() != null && !request.getData().isEmpty()) {
            data.put("data", request.getData());
        }
        documents.put(COLLECTION + "/" + id, data);
        return toResponse(id, data);
    }

    public List<NotificationResponse> findByUserId(String userId) {
        return documents.entrySet().stream()
            .filter(e -> e.getKey().startsWith(COLLECTION + "/"))
            .map(e -> {
                String id = e.getKey().substring(COLLECTION.length() + 1);
                return toResponse(id, e.getValue());
            })
            .filter(r -> userId.equals(r.getUserId()))
            .sorted(Comparator.comparing(NotificationResponse::getCreatedAt).reversed())
            .collect(Collectors.toList());
    }

    public NotificationResponse markRead(String id) {
        Map<String, Object> data = documents.get(COLLECTION + "/" + id);
        if (data == null) {
            return null;
        }
        data.put("read", true);
        return toResponse(id, data);
    }

    public void delete(String id) {
        documents.remove(COLLECTION + "/" + id);
    }

    private static NotificationResponse toResponse(String id, Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        return NotificationResponse.builder()
            .id(id)
            .userId((String) data.get("userId"))
            .title((String) data.get("title"))
            .body((String) data.get("body"))
            .type((String) data.get("type"))
            .read(Boolean.TRUE.equals(data.get("read")))
            .createdAt(Instant.parse((String) data.get("createdAt")))
            .data(data.get("data") != null ? (Map<String, String>) (Map<?, ?>) data.get("data") : null)
            .build();
    }
}
