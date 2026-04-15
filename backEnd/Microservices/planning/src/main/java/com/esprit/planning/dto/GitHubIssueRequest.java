package com.esprit.planning.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a GitHub issue")
public class GitHubIssueRequest {

    @Schema(description = "Issue title", requiredMode = Schema.RequiredMode.REQUIRED, example = "Stalled project: Project X")
    private String title;

    @Schema(description = "Issue body (markdown supported)", example = "No progress update in 7 days for project X.")
    private String body;

    public GitHubIssueRequest() {}

    public GitHubIssueRequest(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public String getTitle() { return title; }
    public String getBody() { return body; }
    public void setTitle(String title) { this.title = title; }
    public void setBody(String body) { this.body = body; }
}
