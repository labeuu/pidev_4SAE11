package tn.esprit.freelanciajob.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import tn.esprit.freelanciajob.Dto.request.NotificationCreateRequest;

/**
 * Calls the Notification microservice to create in-app (Firestore) alerts.
 * Base URL matches {@code notification.service.url} (default: notification service port 8098).
 */
@FeignClient(
        name = "notification",
        url = "${notification.service.url:http://localhost:8098}",
        path = "/api/notifications"
)
public interface NotificationClient {

    @PostMapping
    ResponseEntity<Void> create(@RequestBody NotificationCreateRequest request);
}
