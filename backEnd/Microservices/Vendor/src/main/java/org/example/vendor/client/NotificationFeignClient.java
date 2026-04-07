package org.example.vendor.client;

import org.example.vendor.dto.notification.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Appels vers le microservice Notification (push / in-app).
 */
@FeignClient(name = "notification", path = "/api/notifications")
public interface NotificationFeignClient {

    @PostMapping
    ResponseEntity<?> create(@RequestBody NotificationRequestDto request);
}
