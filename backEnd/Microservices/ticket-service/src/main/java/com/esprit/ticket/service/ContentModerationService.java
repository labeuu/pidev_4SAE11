package com.esprit.ticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Profanity filtering via PurgoMalum ({@code GET /plain}) — one round-trip; returns censored plain text.
 *
 * @see <a href="https://www.purgomalum.com">PurgoMalum</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final RestTemplate restTemplate;

    @Value("${app.moderation.base-url}")
    private String moderationBaseUrl;

    @Value("${app.moderation.enabled:true}")
    private boolean moderationEnabled;

    public String censorIfProfane(String input) {
        if (!moderationEnabled || input == null || input.isBlank()) {
            return input;
        }
        try {
            String base = moderationBaseUrl.replaceAll("/$", "");
            URI uri = UriComponentsBuilder.fromUriString(base + "/plain")
                    .queryParam("text", input)
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "FreelanciaTicketService/1.0");
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Moderation service returned {} or empty body; skipping filter.", response.getStatusCode());
                return input;
            }
            String filtered = response.getBody().trim();
            return filtered.isEmpty() ? input : filtered;
        } catch (Exception e) {
            log.warn("Profanity filter skipped (moderation unreachable): {}", e.toString());
            return input;
        }
    }
}
