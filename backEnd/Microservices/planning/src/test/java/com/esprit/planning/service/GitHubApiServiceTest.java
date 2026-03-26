package com.esprit.planning.service;

import com.esprit.planning.dto.GitHubBranchDto;
import com.esprit.planning.dto.GitHubCommitDto;
import com.esprit.planning.dto.GitHubIssueRequest;
import com.esprit.planning.dto.GitHubIssueResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GitHubApiService. Verifies isEnabled, getBranches, getCommits, getLatestCommit, createIssue
 * with mocked RestTemplate. When disabled or token empty, isEnabled returns false; API methods return empty/null.
 */
class GitHubApiServiceTest {

    @Test
    void isEnabled_whenTokenEmpty_returnsFalse() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "", "", false);
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_whenDisabled_returnsFalse() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "token", "", false);
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_whenEnabledAndTokenSet_returnsTrue() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "token", "", true);
        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_whenTokenHasSpaces_trimmedAndEnabled() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "  token  ", "", true);
        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void getBranches_whenDisabled_returnsEmptyList() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "", "", false);
        List<GitHubBranchDto> result = service.getBranches("owner", "repo");
        assertThat(result).isEmpty();
    }

    @Test
    void getBranches_whenEnabled_returnsBranchesFromApi() {
        RestTemplate rt = mock(RestTemplate.class);
        GitHubBranchDto branch = new GitHubBranchDto();
        branch.setName("main");
        when(rt.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(branch)));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        List<GitHubBranchDto> result = service.getBranches("owner", "repo");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("main");
    }

    @Test
    void getBranches_whenApiReturnsNullBody_returnsEmptyList() {
        RestTemplate rt = mock(RestTemplate.class);
        when(rt.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(null));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        List<GitHubBranchDto> result = service.getBranches("o", "r");

        assertThat(result).isEmpty();
    }

    @Test
    void getBranches_whenApiThrows_returnsEmptyList() {
        RestTemplate rt = mock(RestTemplate.class);
        when(rt.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Network error"));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        List<GitHubBranchDto> result = service.getBranches("o", "r");

        assertThat(result).isEmpty();
    }

    @Test
    void getLatestCommit_whenDisabled_returnsNull() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "", "", false);
        GitHubCommitDto result = service.getLatestCommit("o", "r", null);
        assertThat(result).isNull();
    }

    @Test
    void getLatestCommit_whenEnabledWithBranch_returnsCommit() {
        RestTemplate rt = mock(RestTemplate.class);
        GitHubCommitDto commit = new GitHubCommitDto();
        commit.setSha("abc123");
        when(rt.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(commit)));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        GitHubCommitDto result = service.getLatestCommit("o", "r", "main");

        assertThat(result).isNotNull();
        assertThat(result.getSha()).isEqualTo("abc123");
    }

    @Test
    void getLatestCommit_whenEnabledEmptyList_returnsNull() {
        RestTemplate rt = mock(RestTemplate.class);
        when(rt.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of()));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        GitHubCommitDto result = service.getLatestCommit("o", "r", null);

        assertThat(result).isNull();
    }

    @Test
    void getCommits_whenDisabled_returnsEmptyList() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "", "", false);
        List<GitHubCommitDto> result = service.getCommits("o", "r", null, 30);
        assertThat(result).isEmpty();
    }

    @Test
    void getCommits_whenEnabled_returnsCommitsFromApi() {
        RestTemplate rt = mock(RestTemplate.class);
        GitHubCommitDto c = new GitHubCommitDto();
        c.setSha("def");
        when(rt.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(c)));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        List<GitHubCommitDto> result = service.getCommits("o", "r", "main", 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSha()).isEqualTo("def");
    }

    @Test
    void createIssue_whenDisabled_returnsNull() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "", "", false);
        GitHubIssueResponseDto result = service.createIssue("o", "r", new GitHubIssueRequest("Title", "Body"));
        assertThat(result).isNull();
    }

    @Test
    void createIssue_whenTitleBlank_returnsNull() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "token", "", true);
        GitHubIssueResponseDto result = service.createIssue("o", "r", new GitHubIssueRequest("  ", "Body"));
        assertThat(result).isNull();
    }

    @Test
    void createIssue_whenRequestNull_returnsNull() {
        GitHubApiService service = new GitHubApiService(mock(RestTemplate.class), "token", "", true);
        GitHubIssueResponseDto result = service.createIssue("o", "r", null);
        assertThat(result).isNull();
    }

    @Test
    void createIssue_whenEnabled_returnsIssueFromApi() {
        RestTemplate rt = mock(RestTemplate.class);
        GitHubIssueResponseDto created = new GitHubIssueResponseDto();
        created.setNumber(42);
        created.setTitle("Bug");
        when(rt.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GitHubIssueResponseDto.class)))
                .thenReturn(ResponseEntity.ok(created));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        GitHubIssueResponseDto result = service.createIssue("o", "r", new GitHubIssueRequest("Bug", "Desc"));

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo(42);
    }

    @Test
    void createIssue_whenApiThrows_returnsNull() {
        RestTemplate rt = mock(RestTemplate.class);
        when(rt.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(GitHubIssueResponseDto.class)))
                .thenThrow(new RestClientException("Auth failed"));

        GitHubApiService service = new GitHubApiService(rt, "token", "", true);
        GitHubIssueResponseDto result = service.createIssue("o", "r", new GitHubIssueRequest("T", "B"));

        assertThat(result).isNull();
    }
}
