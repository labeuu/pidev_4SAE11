package com.esprit.ticket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final RestTemplate restTemplate;

    @Value("${app.moderation.base-url}")
    private String moderationBaseUrl;

    public String censorIfProfane(String input) {
        if (input == null || input.isBlank()) return input;
        try {
            String base = moderationBaseUrl.replaceAll("/$", "");
            String encoded = UriUtils.encodeQueryParam(input, StandardCharsets.UTF_8);
            String containsUrl = base + "/containsprofanity?text=" + encoded;
            String contains = restTemplate.getForObject(containsUrl, String.class);
            if (contains == null) return input;
            boolean profane = "true".equalsIgnoreCase(contains.trim());
            if (!profane) return input;

            String plainUrl = base + "/plain?text=" + encoded;
            String censored = restTemplate.getForObject(plainUrl, String.class);
            return (censored == null || censored.isBlank()) ? input : censored;
        } catch (Exception e) {
            // Best-effort moderation: never fail ticket creation due to moderation outage.
            return input;
        }
    }
}

