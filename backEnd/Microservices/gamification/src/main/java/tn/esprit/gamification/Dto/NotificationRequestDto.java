package tn.esprit.gamification.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** Body for POST /api/notifications on the Notification microservice. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDto {

    private String userId;
    private String title;
    private String body;
    private String type;
    private Map<String, String> data;
}
