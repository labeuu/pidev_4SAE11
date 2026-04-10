package com.esprit.planning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "GitHub branch info (from GitHub API)")
public class GitHubBranchDto {

    @Schema(description = "Branch name")
    private String name;

    @JsonProperty("commit")
    @Schema(description = "Latest commit SHA on this branch")
    private CommitRef commitRef;

    public GitHubBranchDto() {}

    public GitHubBranchDto(String name, CommitRef commitRef) {
        this.name = name;
        this.commitRef = commitRef;
    }

    public String getName() { return name; }
    public CommitRef getCommitRef() { return commitRef; }
    public void setName(String name) { this.name = name; }
    public void setCommitRef(CommitRef commitRef) { this.commitRef = commitRef; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommitRef {
        @JsonProperty("sha")
        private String sha;

        public CommitRef() {}
        public CommitRef(String sha) { this.sha = sha; }

        public String getSha() { return sha; }
        public void setSha(String sha) { this.sha = sha; }
    }
}
