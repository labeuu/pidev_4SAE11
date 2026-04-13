package com.esprit.planning.dto;

import java.util.Map;

/**
 * Request body for the Notification microservice (create notification).
 * Used when Planning calls the Notification service via Feign.
 */
public class NotificationRequestDto {

    private String userId;
    private String title;
    private String body;
    private String type;
    private Map<String, String> data;

    public NotificationRequestDto() {}

    public NotificationRequestDto(String userId, String title, String body, String type, Map<String, String> data) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.data = data;
    }

    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getType() { return type; }
    public Map<String, String> getData() { return data; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }
    public void setType(String type) { this.type = type; }
    public void setData(Map<String, String> data) { this.data = data; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String userId;
        private String title;
        private String body;
        private String type;
        private Map<String, String> data;

        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder body(String body) { this.body = body; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder data(Map<String, String> data) { this.data = data; return this; }

        public NotificationRequestDto build() {
            return new NotificationRequestDto(userId, title, body, type, data);
        }
    }
}
