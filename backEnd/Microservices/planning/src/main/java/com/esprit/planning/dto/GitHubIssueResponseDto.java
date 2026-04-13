package com.esprit.planning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "GitHub issue creation response")
public class GitHubIssueResponseDto {

    @Schema(description = "Issue number")
    private Integer number;

    @JsonProperty("html_url")
    @Schema(description = "URL to the issue on GitHub")
    private String htmlUrl;

    @Schema(description = "Issue title")
    private String title;

    @Schema(description = "Issue state")
    private String state;

    public GitHubIssueResponseDto() {}

    public GitHubIssueResponseDto(Integer number, String htmlUrl, String title, String state) {
        this.number = number;
        this.htmlUrl = htmlUrl;
        this.title = title;
        this.state = state;
    }

    public Integer getNumber() { return number; }
    public String getHtmlUrl() { return htmlUrl; }
    public String getTitle() { return title; }
    public String getState() { return state; }

    public void setNumber(Integer number) { this.number = number; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public void setTitle(String title) { this.title = title; }
    public void setState(String state) { this.state = state; }
}
