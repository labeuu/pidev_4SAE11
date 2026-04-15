package com.esprit.planning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "GitHub commit info (from GitHub API)")
public class GitHubCommitDto {

    @Schema(description = "Commit SHA")
    private String sha;

    @JsonProperty("html_url")
    @Schema(description = "URL to the commit on GitHub")
    private String htmlUrl;

    @JsonProperty("commit")
    @Schema(description = "Commit details")
    private CommitDetail commit;

    public GitHubCommitDto() {}

    public GitHubCommitDto(String sha, String htmlUrl, CommitDetail commit) {
        this.sha = sha;
        this.htmlUrl = htmlUrl;
        this.commit = commit;
    }

    public String getSha() { return sha; }
    public String getHtmlUrl() { return htmlUrl; }
    public CommitDetail getCommit() { return commit; }
    public void setSha(String sha) { this.sha = sha; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public void setCommit(CommitDetail commit) { this.commit = commit; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitDetail {
        @Schema(description = "Commit message")
        private String message;

        @JsonProperty("author")
        @Schema(description = "Author info")
        private AuthorDetail author;

        public CommitDetail() {}
        public CommitDetail(String message, AuthorDetail author) { this.message = message; this.author = author; }

        public String getMessage() { return message; }
        public AuthorDetail getAuthor() { return author; }
        public void setMessage(String message) { this.message = message; }
        public void setAuthor(AuthorDetail author) { this.author = author; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorDetail {
        @Schema(description = "Author name")
        private String name;

        @Schema(description = "Commit date (ISO-8601)")
        private String date;

        public AuthorDetail() {}
        public AuthorDetail(String name, String date) { this.name = name; this.date = date; }

        public String getName() { return name; }
        public String getDate() { return date; }
        public void setName(String name) { this.name = name; }
        public void setDate(String date) { this.date = date; }
    }
}
