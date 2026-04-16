package tn.esprit.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chat translation via free APIs (no keys). MyMemory + Lingva fallback.
 * MyMemory often returns error prose inside {@code translatedText}; we detect that and never show it in the UI.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TranslationService {

    private static final String MYMEMORY_URL = "https://api.mymemory.translated.net/get";
    private static final String LINGVA_BASE = "https://lingva.ml/api/v1";
    private static final int MAX_CHUNK = 480;

    /** MyMemory is picky: use RFC3066-style codes where needed (see their LANGPAIR docs). */
    private static final Map<String, String> MYMEMORY_TARGET_FIX = Map.ofEntries(
            Map.entry("zh", "zh-CN"),
            Map.entry("pt", "pt-PT")
    );

    private static final Map<String, String> MYMEMORY_SOURCE_FIX = Map.ofEntries(
            Map.entry("zh", "zh-CN"),
            Map.entry("pt", "pt-PT")
    );

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public String translate(String text, String targetLang, String sourceLang) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String targetNorm = normalizeUiLangCode(targetLang);
        String mySrc = mapMyMemorySource(sourceLang);
        String myTgt = mapMyMemoryTarget(targetNorm);

        List<String> chunks = splitByMaxLength(text, MAX_CHUNK);
        StringBuilder result = new StringBuilder();

        for (String chunk : chunks) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(translateOneChunk(chunk, mySrc, myTgt, targetNorm, sourceLang));
        }
        return result.toString();
    }

    private String translateOneChunk(String chunk, String myMemorySource, String myMemoryTarget,
                                     String lingvaTarget, String rawSourceLang) {
        String fromMemory = translateChunkMyMemory(chunk, myMemorySource, myMemoryTarget);
        if (!isBadMyMemoryResult(fromMemory, chunk)) {
            return fromMemory;
        }

        String fromLingva = translateChunkLingva(chunk, lingvaTarget, rawSourceLang);
        if (!isLikelyApiErrorString(fromLingva)) {
            return fromLingva;
        }
        return chunk;
    }

    private String normalizeUiLangCode(String code) {
        if (code == null || code.isBlank()) {
            return "en";
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private String mapMyMemorySource(String sourceLang) {
        if (sourceLang == null || sourceLang.isBlank() || "auto".equalsIgnoreCase(sourceLang)) {
            return "en";
        }
        String key = sourceLang.trim().toLowerCase(Locale.ROOT);
        return MYMEMORY_SOURCE_FIX.getOrDefault(key, key);
    }

    private String mapMyMemoryTarget(String targetLang) {
        return MYMEMORY_TARGET_FIX.getOrDefault(targetLang, targetLang);
    }

    private String translateChunkMyMemory(String text, String sourceLang, String targetLang) {
        try {
            String langpair = sourceLang.toUpperCase(Locale.ROOT) + "|" + targetLang.toUpperCase(Locale.ROOT);
            String url = UriComponentsBuilder.fromUriString(MYMEMORY_URL)
                    .queryParam("q", text)
                    .queryParam("langpair", langpair)
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            int responseStatus = root.path("responseStatus").asInt(200);
            if (responseStatus != 200) {
                log.debug("[TranslationService] MyMemory responseStatus={}", responseStatus);
                return text;
            }
            String translated = root.path("responseData").path("translatedText").asText(null);
            if (translated == null || translated.isBlank()) {
                return text;
            }
            if (isLikelyApiErrorString(translated)) {
                return text;
            }
            return decodeTranslationText(translated);
        } catch (Exception e) {
            log.warn("[TranslationService] MyMemory chunk failed: {}", e.getMessage());
            return text;
        }
    }

    /**
     * Some providers return URL-encoded fragments in JSON (e.g. {@code %20} instead of spaces).
     */
    private String decodeTranslationText(String s) {
        if (s == null || !s.contains("%")) {
            return s;
        }
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return s.replace("%20", " ");
        }
    }

    /**
     * MyMemory puts error messages in {@code translatedText} (HTTP 200). Never treat those as translation.
     */
    private boolean isLikelyApiErrorString(String s) {
        if (s == null || s.length() < 12) {
            return false;
        }
        String u = s.toUpperCase(Locale.ROOT);
        if (u.contains("INVALID LANGUAGE PAIR")) {
            return true;
        }
        if (u.contains("LANGPAIR")) {
            return true;
        }
        if (u.contains("QUERY LENGTH LIMIT")) {
            return true;
        }
        if (u.contains("MYMEMORY") && u.contains("API")) {
            return true;
        }
        return false;
    }

    private boolean isBadMyMemoryResult(String translated, String originalChunk) {
        if (translated == null || translated.isBlank()) {
            return true;
        }
        if (translated.equals(originalChunk)) {
            return true;
        }
        return isLikelyApiErrorString(translated);
    }

    private String translateChunkLingva(String text, String targetLang, String rawSourceLang) {
        try {
            String src = (rawSourceLang == null || rawSourceLang.isBlank() || "auto".equalsIgnoreCase(rawSourceLang))
                    ? "auto"
                    : rawSourceLang.trim().toLowerCase(Locale.ROOT);
            String tgt = targetLang;
            String encodedText = UriUtils.encodePathSegment(text, StandardCharsets.UTF_8);
            String url = LINGVA_BASE + "/" + src + "/" + tgt + "/" + encodedText;

            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank()) {
                return text;
            }
            JsonNode root = objectMapper.readTree(response);
            if (root.has("error")) {
                return text;
            }
            String translated = root.path("translation").asText(null);
            if (translated == null || translated.isBlank() || isLikelyApiErrorString(translated)) {
                return text;
            }
            return decodeTranslationText(translated);
        } catch (Exception e) {
            log.debug("[TranslationService] Lingva chunk failed: {}", e.getMessage());
            return text;
        }
    }

    private List<String> splitByMaxLength(String text, int maxChunk) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= maxChunk) {
            chunks.add(text);
            return chunks;
        }

        String remaining = text;
        while (remaining.length() > maxChunk) {
            int cutAt = maxChunk;
            int best = Math.max(
                    Math.max(remaining.lastIndexOf('.', cutAt), remaining.lastIndexOf('!', cutAt)),
                    Math.max(remaining.lastIndexOf('?', cutAt), remaining.lastIndexOf('\n', cutAt))
            );
            if (best > 50) {
                cutAt = best + 1;
            } else {
                int spaceAt = remaining.lastIndexOf(' ', cutAt);
                if (spaceAt > 50) {
                    cutAt = spaceAt;
                }
            }
            chunks.add(remaining.substring(0, cutAt).trim());
            remaining = remaining.substring(cutAt).trim();
        }
        if (!remaining.isEmpty()) {
            chunks.add(remaining);
        }
        return chunks;
    }
}
