package org.example.offer.client;

import org.example.offer.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the Notification microservice.
 * Creates in-app notifications when clients ask questions on offers.
 */
@FeignClient(name = "notification", url = "${service.notification.url:http://localhost:8098}", path = "/api/notifications")
public interface NotificationClient {

    @PostMapping
    ResponseEntity<?> create(@RequestBody NotificationRequestDto request);
}
