package com.esprit.task.config;

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

    @Value("${server.port:8091}")
    private String serverPort;

    @Bean
    public OpenAPI taskOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tasks & Subtasks API")
                        .description("""
                                REST API for **Tasks and Subtasks** microservice.
                                
                                - **Tasks**: CRUD, filters, board, overdue, stats (including extended metrics), weekly PDF report, calendar events.
                                - **Schedulers**: daily overdue reminder notifications (freelancers with assigned overdue tasks/subtasks), priority escalation, cancelled-task purge (see `task.scheduler.*` properties).
                                - **Task Comments**: Create and manage comments on tasks.
                                
                                Tasks integrate with the Planning calendar via dueDate.
                                """)
                        .version("1.0")
                        .contact(new Contact()
                                .name("Task Service")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local server")));
    }
}
