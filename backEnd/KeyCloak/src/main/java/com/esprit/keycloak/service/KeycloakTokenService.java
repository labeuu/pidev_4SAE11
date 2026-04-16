package com.esprit.keycloak.service;

import com.esprit.keycloak.config.KeycloakProperties;
import com.esprit.keycloak.dto.TokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Proxies token requests to Keycloak (resource owner password grant).
 * Clients can POST /api/auth/token with username/password to get JWT.
 */
@Service
public class KeycloakTokenService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakTokenService.class);
    private final KeycloakProperties keycloakProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KeycloakTokenService(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    public TokenResponse refreshToken(String refreshToken) {
        String url = keycloakProperties.getAuthServerUrl() + "/realms/" + keycloakProperties.getRealm()
            + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", keycloakProperties.getResource());
        body.add("refresh_token", refreshToken);
        String secret = keycloakProperties.getCredentials().getSecret();
        if (secret != null && !secret.isBlank()) {
            body.add("client_secret", secret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalArgumentException("Token refresh failed: " + response.getStatusCode());
        }

        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            return new TokenResponse(
                node.path("access_token").asText(null),
                node.has("refresh_token") ? node.path("refresh_token").asText() : null,
                node.path("token_type").asText("Bearer"),
                node.has("expires_in") ? node.path("expires_in").asInt() : null,
                node.has("refresh_expires_in") ? node.path("refresh_expires_in").asInt() : null,
                node.has("scope") ? node.path("scope").asText() : null
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse token refresh response", e);
        }
    }

    public TokenResponse getToken(String username, String password) {
        String url = keycloakProperties.getAuthServerUrl() + "/realms/" + keycloakProperties.getRealm()
            + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakProperties.getResource());
        body.add("username", username);
        body.add("password", password);
        String secret = keycloakProperties.getCredentials().getSecret();
        if (secret != null && !secret.isBlank()) {
            body.add("client_secret", secret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Token request failed: " + response.getStatusCode());
            }
            responseBody = response.getBody();
        } catch (HttpStatusCodeException e) {
            String message = parseKeycloakError(e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                log.debug("Keycloak returned 401 for token request: {}", message);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
            }
            if (e.getStatusCode().value() == 400) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
            throw new ResponseStatusException(e.getStatusCode(), message);
        } catch (ResourceAccessException e) {
            log.warn("Keycloak unreachable at {}: {}", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Authentication service unavailable. Ensure Keycloak is running (default port 8080; see keycloak.auth-server-url).");
        }

        try {
            JsonNode node = objectMapper.readTree(responseBody);
            return new TokenResponse(
                node.path("access_token").asText(null),
                node.has("refresh_token") ? node.path("refresh_token").asText() : null,
                node.path("token_type").asText("Bearer"),
                node.has("expires_in") ? node.path("expires_in").asInt() : null,
                node.has("refresh_expires_in") ? node.path("refresh_expires_in").asInt() : null,
                node.has("scope") ? node.path("scope").asText() : null
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse token response", e);
        }
    }

    private String parseKeycloakError(String body) {
        if (body == null || body.isBlank()) return "Invalid credentials or request.";
        try {
            JsonNode node = objectMapper.readTree(body);
            String desc = node.path("error_description").asText("");
            if (!desc.isBlank()) return desc;
            String err = node.path("error").asText("");
            return err.isBlank() ? "Invalid credentials or request." : err;
        } catch (Exception e) {
            return "Invalid credentials or request.";
        }
    }
}
