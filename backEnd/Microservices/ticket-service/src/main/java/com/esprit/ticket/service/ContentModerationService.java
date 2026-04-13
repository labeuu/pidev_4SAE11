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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Profanity checks via PurgoMalum {@code containsprofanity}; optional censor via {@code /plain}.
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

    /** When true, reject text that contains profanity; when false, replace using /plain. */
    @Value("${app.moderation.reject-on-profanity:true}")
    private boolean rejectOnProfanity;

    /**
     * Validates and returns safe text for persistence. Throws {@link ResponseStatusException} 400 when
     * {@code reject-on-profanity} is true and profanity is detected.
     */
    public String validateAndPrepareText(String input) {
        if (!moderationEnabled || input == null) {
            return input;
        }
        if (input.isBlank()) {
            return input;
        }
        if (containsProfanity(input)) {
            if (rejectOnProfanity) {
                throw new ResponseStatusException(BAD_REQUEST, "Message contains inappropriate language.");
            }
            return censorPlain(input);
        }
        return input;
    }

    public boolean containsProfanity(String text) {
        if (!moderationEnabled || text == null || text.isBlank()) {
            return false;
        }
        try {
            String base = moderationBaseUrl.replaceAll("/$", "");
            URI uri = UriComponentsBuilder.fromUriString(base + "/containsprofanity")
                    .queryParam("text", text)
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "FreelanciaTicketService/1.0");
            ResponseEntity<String> response =
                    restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("containsprofanity returned {} or empty body; treating as clean.", response.getStatusCode());
                return false;
            }
            return Boolean.parseBoolean(response.getBody().trim());
        } catch (Exception e) {
            log.warn("Profanity check skipped (moderation unreachable): {}", e.toString());
            return false;
        }
    }

    private String censorPlain(String input) {
        try {
            String base = moderationBaseUrl.replaceAll("/$", "");
            URI uri = UriComponentsBuilder.fromUriString(base + "/plain")
                    .queryParam("text", input)
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "FreelanciaTicketService/1.0");
            ResponseEntity<String> response =
                    restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return input;
            }
            String filtered = response.getBody().trim();
            return filtered.isEmpty() ? input : filtered;
        } catch (Exception e) {
            log.warn("Censor plain skipped: {}", e.toString());
            return input;
        }
    }

    /** @deprecated use {@link #validateAndPrepareText(String)} */
    @Deprecated
    public String censorIfProfane(String input) {
        return validateAndPrepareText(input);
    }
}
