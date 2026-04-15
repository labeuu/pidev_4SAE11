package tn.esprit.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TranslationService {

    private static final String MYMEMORY_URL = "https://api.mymemory.translated.net/get";
    private static final int MAX_CHUNK = 480;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Translates text to the target language.
     * Splits long text into chunks to respect the MyMemory free-tier 500-char limit.
     */
    public String translate(String text, String targetLang, String sourceLang) {
        if (text == null || text.isBlank()) return text;
        if (sourceLang == null || sourceLang.isBlank() || "auto".equalsIgnoreCase(sourceLang)) {
            sourceLang = "en";
        }

        List<String> chunks = splitText(text);
        StringBuilder result = new StringBuilder();

        for (String chunk : chunks) {
            if (!result.isEmpty()) result.append(" ");
            result.append(translateChunk(chunk, targetLang, sourceLang));
        }

        return result.toString();
    }

    private String translateChunk(String text, String targetLang, String sourceLang) {
        try {
            String url = UriComponentsBuilder.fromUriString(MYMEMORY_URL)
                    .queryParam("q", text)
                    .queryParam("langpair", sourceLang + "|" + targetLang)
                    .build()
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            String translated = root.path("responseData").path("translatedText").asText(null);
            return (translated == null || translated.isBlank()) ? text : translated;
        } catch (Exception e) {
            log.warn("[TranslationService] chunk translation failed: {}", e.getMessage());
            return text;
        }
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK) {
            chunks.add(text);
            return chunks;
        }

        String remaining = text;
        while (remaining.length() > MAX_CHUNK) {
            int cutAt = MAX_CHUNK;
            int best = Math.max(
                    Math.max(remaining.lastIndexOf('.', cutAt), remaining.lastIndexOf('!', cutAt)),
                    Math.max(remaining.lastIndexOf('?', cutAt), remaining.lastIndexOf('\n', cutAt))
            );
            if (best > 50) {
                cutAt = best + 1;
            } else {
                int spaceAt = remaining.lastIndexOf(' ', cutAt);
                if (spaceAt > 50) cutAt = spaceAt;
            }
            chunks.add(remaining.substring(0, cutAt).trim());
            remaining = remaining.substring(cutAt).trim();
        }
        if (!remaining.isEmpty()) chunks.add(remaining);
        return chunks;
    }
}
