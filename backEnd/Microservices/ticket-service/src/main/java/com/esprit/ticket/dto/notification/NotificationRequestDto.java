package com.esprit.ticket.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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
