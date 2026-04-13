package com.esprit.notification.service;

import com.esprit.notification.dto.NotificationRequest;
import com.esprit.notification.dto.NotificationResponse;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final String COLLECTION = "notifications";

    private final Firestore firestore;
    private final InMemoryNotificationStore memoryStore;
    private final boolean useFirestore;

    public NotificationService(
        ObjectProvider<Firestore> firestoreProvider,
        InMemoryNotificationStore memoryStore,
        @Value("${notification.firebase.enabled:false}") boolean firebaseEnabled
    ) {
        this.memoryStore = memoryStore;
        Firestore fs = firestoreProvider.getIfAvailable();
        if (firebaseEnabled) {
            if (fs == null) {
                throw new IllegalStateException(
                    "notification.firebase.enabled=true but Firestore bean is missing. "
                        + "Set GOOGLE_APPLICATION_CREDENTIALS or notification.firebase.credentials-path.");
            }
            this.firestore = fs;
            this.useFirestore = true;
        } else {
            this.firestore = null;
            this.useFirestore = false;
        }
    }

    public NotificationResponse create(NotificationRequest request) {
        if (!useFirestore) {
            return memoryStore.create(request);
        }
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

        try {
            DocumentReference ref = firestore.collection(COLLECTION).add(data).get();
            DocumentSnapshot snap = ref.get().get();
            return toResponse(ref.getId(), snap.getData());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    public List<NotificationResponse> findByUserId(String userId) {
        if (!useFirestore) {
            return memoryStore.findByUserId(userId);
        }
        try {
            return firestore.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(d -> toResponse(d.getId(), d.getData()))
                .sorted(Comparator.comparing(NotificationResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to list notifications", e);
        }
    }

    public NotificationResponse markRead(String id) {
        if (!useFirestore) {
            return memoryStore.markRead(id);
        }
        try {
            DocumentReference ref = firestore.collection(COLLECTION).document(id);
            ref.update("read", true).get();
            DocumentSnapshot snap = ref.get().get();
            return toResponse(id, snap.getData());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to mark notification as read", e);
        }
    }

    public void delete(String id) {
        if (!useFirestore) {
            memoryStore.delete(id);
            return;
        }
        try {
            firestore.collection(COLLECTION).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to delete notification", e);
        }
    }

    private static NotificationResponse toResponse(String id, Map<String, Object> data) {
        if (data == null) return null;
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
