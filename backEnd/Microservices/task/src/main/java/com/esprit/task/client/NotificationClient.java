package com.esprit.task.client;

import com.esprit.task.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the Notification microservice.
 * Sends notifications when task status is updated.
 */
@FeignClient(name = "notification", path = "/api/notifications")
public interface NotificationClient {

    @PostMapping
    ResponseEntity<?> create(@RequestBody NotificationRequestDto request);
}
