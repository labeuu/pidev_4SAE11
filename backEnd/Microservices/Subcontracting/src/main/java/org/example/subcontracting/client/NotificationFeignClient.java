package org.example.subcontracting.client;

import org.example.subcontracting.client.dto.NotificationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification", path = "/api/notifications")
public interface NotificationFeignClient {

    @PostMapping
    void sendNotification(@RequestBody NotificationRequestDto request);
}
