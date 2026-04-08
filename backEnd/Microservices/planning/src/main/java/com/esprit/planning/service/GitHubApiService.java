package com.esprit.planning.service;

import com.esprit.planning.dto.GitHubBranchDto;
import com.esprit.planning.dto.GitHubCommitDto;
import com.esprit.planning.dto.GitHubIssueRequest;
import com.esprit.planning.dto.GitHubIssueResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Integrates with GitHub REST API: list branches, get latest commit, create issue.
 * Token is read from github.token, GITHUB_TOKEN env, or github.token-file (e.g. githubToken.txt).
 */
@Service
@Slf4j
public class GitHubApiService {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate;
    private final String token;
    private final boolean enabled;

    public GitHubApiService(
            RestTemplate restTemplate,
            @Value("${github.token:}") String token,
            @Value("${github.token-file:}") String tokenFile,
            @Value("${github.enabled:false}") boolean enabled) {
        this.restTemplate = restTemplate;
        String resolved = token != null ? token.trim() : "";
        if (resolved.isEmpty() && tokenFile != null && !tokenFile.isBlank()) {
            resolved = loadTokenFromFile(tokenFile.trim());
        }
        this.token = resolved;
        this.enabled = enabled;
    }

    // Performs load token from file.
    private String loadTokenFromFile(String pathStr) {
        try {
            Path path = Paths.get(pathStr).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                String t = Files.readString(path).trim();
                if (!t.isEmpty()) {
                    log.info("GitHub token loaded from file: {}", path);
                    return t;
                }
            }
        } catch (Exception e) {
            log.debug("Could not load GitHub token from {}: {}", pathStr, e.getMessage());
        }
        return "";
    }

    /** Returns true if GitHub integration is enabled and a token is configured. */
    public boolean isEnabled() {
        return enabled && !token.isEmpty();
    }

    // Builds headers for GitHub API calls.
    private HttpHeaders authHeaders(boolean includeToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.parseMediaType("application/vnd.github.v3+json")));
        if (includeToken && !token.isEmpty()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    /**
     * List branches for a repository. Returns empty list on error or when disabled.
     */
    public List<GitHubBranchDto> getBranches(String owner, String repo) {
        if (!enabled) return Collections.emptyList();
        String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/branches";
        try {
            ResponseEntity<List<GitHubBranchDto>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(true)),
                    new ParameterizedTypeReference<>() {}
            );
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (HttpStatusCodeException e) {
            if ((e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) && !token.isEmpty()) {
                try {
                    ResponseEntity<List<GitHubBranchDto>> resp = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(authHeaders(false)),
                            new ParameterizedTypeReference<>() {}
                    );
                    return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
                } catch (RestClientException retryEx) {
                    log.warn("GitHub getBranches retry (without token) failed for {}/{}: {}", owner, repo, retryEx.getMessage());
                    return Collections.emptyList();
                }
            }
            log.warn("GitHub getBranches failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("GitHub getBranches failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get the latest commit for a branch (or default branch if branch is null/empty).
     * Returns null on error or when disabled.
     */
    public GitHubCommitDto getLatestCommit(String owner, String repo, String branch) {
        if (!enabled) return null;
        String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/commits?per_page=1";
        if (branch != null && !branch.isBlank()) {
            url += "&sha=" + branch;
        }
        try {
            ResponseEntity<List<GitHubCommitDto>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(true)),
                    new ParameterizedTypeReference<>() {}
            );
            List<GitHubCommitDto> list = resp.getBody();
            return (list != null && !list.isEmpty()) ? list.get(0) : null;
        } catch (HttpStatusCodeException e) {
            if ((e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) && !token.isEmpty()) {
                try {
                    ResponseEntity<List<GitHubCommitDto>> resp = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(authHeaders(false)),
                            new ParameterizedTypeReference<>() {}
                    );
                    List<GitHubCommitDto> list = resp.getBody();
                    return (list != null && !list.isEmpty()) ? list.get(0) : null;
                } catch (RestClientException retryEx) {
                    log.warn("GitHub getLatestCommit retry (without token) failed for {}/{}: {}", owner, repo, retryEx.getMessage());
                    return null;
                }
            }
            log.warn("GitHub getLatestCommit failed for {}/{}: {}", owner, repo, e.getMessage());
            return null;
        } catch (RestClientException e) {
            log.warn("GitHub getLatestCommit failed for {}/{}: {}", owner, repo, e.getMessage());
            return null;
        }
    }

    /**
     * List commits for a repository (full history or for a branch). Returns empty list on error or when disabled.
     */
    public List<GitHubCommitDto> getCommits(String owner, String repo, String branch, int perPage) {
        if (!enabled) return Collections.emptyList();
        String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/commits?per_page=" + Math.min(Math.max(perPage, 1), 100);
        if (branch != null && !branch.isBlank()) {
            url += "&sha=" + branch;
        }
        try {
            ResponseEntity<List<GitHubCommitDto>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders(true)),
                    new ParameterizedTypeReference<>() {}
            );
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (HttpStatusCodeException e) {
            if ((e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) && !token.isEmpty()) {
                try {
                    ResponseEntity<List<GitHubCommitDto>> resp = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(authHeaders(false)),
                            new ParameterizedTypeReference<>() {}
                    );
                    return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
                } catch (RestClientException retryEx) {
                    log.warn("GitHub getCommits retry (without token) failed for {}/{}: {}", owner, repo, retryEx.getMessage());
                    return Collections.emptyList();
                }
            }
            log.warn("GitHub getCommits failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("GitHub getCommits failed for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Create an issue in the given repository. Returns null on error or when disabled.
     */
    public GitHubIssueResponseDto createIssue(String owner, String repo, GitHubIssueRequest request) {
        if (!enabled || request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            return null;
        }
        String url = GITHUB_API_BASE + "/repos/" + owner + "/" + repo + "/issues";
        try {
            ResponseEntity<GitHubIssueResponseDto> resp = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders(true)),
                    GitHubIssueResponseDto.class
            );
            return resp.getBody();
        } catch (RestClientException e) {
            log.warn("GitHub createIssue failed for {}/{}: {}", owner, repo, e.getMessage());
            return null;
        }
    }
}
