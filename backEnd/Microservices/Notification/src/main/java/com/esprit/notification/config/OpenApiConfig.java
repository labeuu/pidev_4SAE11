package com.esprit.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8098}")
    private String serverPort;

    @Bean
    public OpenAPI notificationOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Notification API")
                .description("""
                    REST API for the **Notification** microservice (Firestore-backed).
                    
                    Other microservices can call this service to create and manage user notifications.
                    - **POST /api/notifications** – create a notification for a user
                    - **GET /api/notifications/user/{userId}** – list notifications for a user
                    - **PATCH /api/notifications/{id}/read** – mark as read
                    - **DELETE /api/notifications/{id}** – delete a notification
                    """)
                .version("1.0")
                .contact(new Contact().name("Notification Service")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local server")));
    }
}
