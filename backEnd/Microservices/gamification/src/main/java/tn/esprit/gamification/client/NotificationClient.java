package tn.esprit.gamification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.esprit.gamification.dto.NotificationRequestDto;

@FeignClient(
        name = "gamificationNotificationFeign",
        url = "${notification.service.url:http://localhost:8098}",
        path = "/api/notifications"
)
public interface NotificationClient {

    @PostMapping
    ResponseEntity<?> create(@RequestBody NotificationRequestDto request);
}
