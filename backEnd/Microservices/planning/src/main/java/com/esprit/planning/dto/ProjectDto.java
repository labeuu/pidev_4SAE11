package com.esprit.planning.dto;

import java.time.LocalDateTime;

/** Project info from Project microservice (for clientId lookup and deadline sync to calendar). */
public class ProjectDto {
    private Long id;
    private Long clientId;
    private String title;
    private LocalDateTime deadline;

    public ProjectDto() {}

    public ProjectDto(Long id, Long clientId, String title, LocalDateTime deadline) {
        this.id = id;
        this.clientId = clientId;
        this.title = title;
        this.deadline = deadline;
    }

    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public String getTitle() { return title; }
    public LocalDateTime getDeadline() { return deadline; }

    public void setId(Long id) { this.id = id; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public void setTitle(String title) { this.title = title; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
}
