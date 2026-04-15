package tn.esprit.freelanciajob.Dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Mirrors {@code com.esprit.notification.dto.NotificationRequest} for Feign calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {

    private String userId;
    private String title;
    private String body;
    private String type;
    private Map<String, String> data;
}
