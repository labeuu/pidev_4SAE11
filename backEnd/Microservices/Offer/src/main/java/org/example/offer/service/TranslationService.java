package org.example.offer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de traduction via Google Cloud Translation API v2.
 * Langues supportées : FR, EN, AR.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranslationService {

    private static final String GOOGLE_TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";

    private final RestTemplate restTemplate;

    @Value("${app.translation.google-api-key:}")
    private String apiKey;

    private static final Set<String> ALLOWED_LANGUAGES = Set.of("fr", "en", "ar");

    /**
     * Traduit une liste de textes vers la langue cible.
     *
     * @param texts          textes à traduire (max 10)
     * @param targetLanguage code langue cible (fr, en, ar)
     * @return liste des textes traduits dans le même ordre
     */
    public List<String> translate(List<String> texts, String targetLanguage) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Translate API key is not configured. Set app.translation.google-api-key.");
            throw new IllegalStateException("Translation service is not configured. Please set app.translation.google-api-key.");
        }
        String target = normalizeTargetLanguage(targetLanguage);
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<String> toTranslate = texts.stream()
                .map(t -> t == null || t.isBlank() ? " " : t)
                .limit(10)
                .collect(Collectors.toList());

        String url = GOOGLE_TRANSLATE_URL + "?key=" + apiKey;
        Map<String, Object> body = new HashMap<>();
        body.put("q", toTranslate);
        body.put("target", target);
        body.put("format", "text");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GoogleTranslateResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    GoogleTranslateResponse.class
            );
            if (response.getBody() == null || response.getBody().getData() == null
                    || response.getBody().getData().getTranslations() == null) {
                return toTranslate;
            }
            return response.getBody().getData().getTranslations().stream()
                    .map(GoogleTranslateResponse.Translation::getTranslatedText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Google Translate API error: {}", e.getMessage());
            throw new RuntimeException("Translation failed: " + e.getMessage(), e);
        }
    }

    private String normalizeTargetLanguage(String target) {
        if (target == null || target.isBlank()) return "en";
        String lower = target.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_LANGUAGES.contains(lower)) return lower;
        if ("french".equals(lower) || "français".equals(lower)) return "fr";
        if ("english".equals(lower) || "anglais".equals(lower)) return "en";
        if ("arabic".equals(lower) || "arabe".equals(lower)) return "ar";
        return "en";
    }

    @Data
    private static class GoogleTranslateResponse {
        private DataWrapper data;

        @Data
        private static class DataWrapper {
            private List<Translation> translations;
        }

        @Data
        private static class Translation {
            @JsonProperty("translatedText")
            private String translatedText;
        }
    }
}
