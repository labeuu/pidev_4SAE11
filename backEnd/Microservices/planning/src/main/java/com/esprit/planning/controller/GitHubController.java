package com.esprit.planning.controller;

import com.esprit.planning.dto.GitHubBranchDto;
import com.esprit.planning.dto.GitHubCommitDto;
import com.esprit.planning.dto.GitHubIssueRequest;
import com.esprit.planning.dto.GitHubIssueResponseDto;
import com.esprit.planning.service.GitHubApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Proxy to GitHub API: branches, latest commit, create issue.
 * Available to all authenticated users (client, freelancer, admin). When GitHub integration is disabled, returns 503 or empty.
 */
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "GitHub", description = "GitHub API integration: list branches, get latest commit, create issues")
public class GitHubController {

    private final GitHubApiService githubApiService;

    /** Returns the list of branches for the given repository. 503 if GitHub integration is disabled. */
    @GetMapping("/repos/{owner}/{repo}/branches")
    @Operation(summary = "List branches", description = "Returns branch list for the given repository. Requires GitHub integration enabled.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "503", description = "GitHub integration disabled")
    public ResponseEntity<List<GitHubBranchDto>> getBranches(
            @Parameter(description = "Repository owner (user or org)") @PathVariable String owner,
            @Parameter(description = "Repository name") @PathVariable String repo) {
        if (!githubApiService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        List<GitHubBranchDto> branches = githubApiService.getBranches(owner, repo);
        return ResponseEntity.ok(branches);
    }

    /** Returns commit history for the repo, optionally for a specific branch; perPage caps the number (1-100). 503 if disabled. */
    @GetMapping("/repos/{owner}/{repo}/commits")
    @Operation(summary = "List commits", description = "Returns commit history for the repo, optionally for a specific branch. Use for full history view.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "503", description = "GitHub integration disabled")
    public ResponseEntity<List<GitHubCommitDto>> getCommits(
            @Parameter(description = "Repository owner") @PathVariable String owner,
            @Parameter(description = "Repository name") @PathVariable String repo,
            @Parameter(description = "Branch name (optional)") @RequestParam(required = false) String branch,
            @Parameter(description = "Number of commits to return (default 30, max 100)") @RequestParam(required = false, defaultValue = "30") Integer perPage) {
        if (!githubApiService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        int limit = perPage != null ? Math.min(Math.max(perPage, 1), 100) : 30;
        List<GitHubCommitDto> commits = githubApiService.getCommits(owner, repo, branch, limit);
        return ResponseEntity.ok(commits);
    }

    /** Returns the latest commit for the repo, optionally for a branch. 404 if none found; 503 if disabled. */
    @GetMapping("/repos/{owner}/{repo}/commits/latest")
    @Operation(summary = "Get latest commit", description = "Returns the latest commit for the given repo, optionally for a specific branch.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "503", description = "GitHub integration disabled")
    public ResponseEntity<GitHubCommitDto> getLatestCommit(
            @Parameter(description = "Repository owner") @PathVariable String owner,
            @Parameter(description = "Repository name") @PathVariable String repo,
            @Parameter(description = "Branch name (optional; default branch if omitted)") @RequestParam(required = false) String branch) {
        if (!githubApiService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        GitHubCommitDto commit = githubApiService.getLatestCommit(owner, repo, branch);
        return commit != null ? ResponseEntity.ok(commit) : ResponseEntity.notFound().build();
    }

    /** Creates a new issue in the repository. 400 if title blank; 503 if disabled; 502 if GitHub request fails. */
    @PostMapping("/repos/{owner}/{repo}/issues")
    @Operation(summary = "Create issue", description = "Creates a new issue in the repository. Available to all roles.")
    @ApiResponse(responseCode = "201", description = "Issue created")
    @ApiResponse(responseCode = "400", description = "Invalid request (e.g. missing title)")
    @ApiResponse(responseCode = "503", description = "GitHub integration disabled")
    public ResponseEntity<GitHubIssueResponseDto> createIssue(
            @Parameter(description = "Repository owner") @PathVariable String owner,
            @Parameter(description = "Repository name") @PathVariable String repo,
            @RequestBody GitHubIssueRequest request) {
        if (!githubApiService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        GitHubIssueResponseDto created = githubApiService.createIssue(owner, repo, request);
        return created != null ? ResponseEntity.status(HttpStatus.CREATED).body(created) : ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    /** Returns whether GitHub integration is enabled (true/false). */
    @GetMapping("/enabled")
    @Operation(summary = "Check if GitHub integration is enabled")
    @ApiResponse(responseCode = "200", description = "Success")
    // Checks whether enabled.
    public ResponseEntity<Boolean> isEnabled() {
        return ResponseEntity.ok(githubApiService.isEnabled());
    }
}
