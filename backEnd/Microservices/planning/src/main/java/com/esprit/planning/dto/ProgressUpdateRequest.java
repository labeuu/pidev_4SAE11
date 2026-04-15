package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Request body for creating or updating a progress update")
public class ProgressUpdateRequest {

    @Schema(description = "ID of the project", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @Schema(description = "ID of the contract (optional; null if no contract yet)", example = "1", nullable = true)
    private Long contractId;

    @Schema(description = "ID of the freelancer who submitted the update", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long freelancerId;

    @Schema(description = "Short title of the progress update", example = "Backend API completed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Detailed description of the progress", example = "Implemented all REST endpoints for user and project management.")
    private String description;

    @Schema(description = "Progress percentage (0-100)", example = "75", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "100")
    private Integer progressPercentage;

    @Schema(description = "Optional: next progress update due date (synced to Google Calendar when enabled)", example = "2025-03-15T14:00:00")
    private LocalDateTime nextUpdateDue;

    @Schema(description = "Optional: GitHub repository URL linked to this update (e.g. https://github.com/owner/repo)", example = "https://github.com/octocat/Hello-World")
    private String githubRepoUrl;

    public ProgressUpdateRequest() {}

    public ProgressUpdateRequest(Long projectId, Long contractId, Long freelancerId, String title,
                                 String description, Integer progressPercentage,
                                 LocalDateTime nextUpdateDue, String githubRepoUrl) {
        this.projectId = projectId;
        this.contractId = contractId;
        this.freelancerId = freelancerId;
        this.title = title;
        this.description = description;
        this.progressPercentage = progressPercentage;
        this.nextUpdateDue = nextUpdateDue;
        this.githubRepoUrl = githubRepoUrl;
    }

    public Long getProjectId() { return projectId; }
    public Long getContractId() { return contractId; }
    public Long getFreelancerId() { return freelancerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public LocalDateTime getNextUpdateDue() { return nextUpdateDue; }
    public String getGithubRepoUrl() { return githubRepoUrl; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public void setNextUpdateDue(LocalDateTime nextUpdateDue) { this.nextUpdateDue = nextUpdateDue; }
    public void setGithubRepoUrl(String githubRepoUrl) { this.githubRepoUrl = githubRepoUrl; }
}
