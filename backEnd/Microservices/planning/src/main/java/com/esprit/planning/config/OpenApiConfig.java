package com.esprit.planning.config;

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

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    // Performs planning open api.
    public OpenAPI planningOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Planning & Tracking API")
                        .description("""
                                REST API for **Gestion Planning and Tracking** microservice.
                                
                                - **Progress Updates**: Create and manage progress updates for projects (title, description, percentage).
                                - **Progress Comments**: Add and manage comments on progress updates.
                                
                                Use **Try it out** on each operation to send requests. Ensure a progress update exists before creating comments (use its `id` as `progressUpdateId`).
                                """)
                        .version("1.0")
                        .contact(new Contact()
                                .name("Planning Service")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local server")));
    }
}
