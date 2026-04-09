package com.esprit.aimodel.service;

import com.esprit.aimodel.dto.AiLiveStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ProviderStatusService {

    private final RestClient ollamaTagsRestClient;
    private final ObjectMapper objectMapper;
    private final String configuredModel;

    public ProviderStatusService(
            @Qualifier("ollamaTagsRestClient") RestClient ollamaTagsRestClient,
            ObjectMapper objectMapper,
            @Value("${spring.ai.ollama.chat.options.model:}") String configuredModel) {
        this.ollamaTagsRestClient = ollamaTagsRestClient;
        this.objectMapper = objectMapper;
        this.configuredModel = configuredModel != null ? configuredModel : "";
    }

    public AiLiveStatus liveStatus() {
        try {
            String body = ollamaTagsRestClient.get().uri("/api/tags").retrieve().body(String.class);
            List<String> names = parseOllamaModelNames(body, objectMapper);
            boolean modelReady = modelIsListed(names, configuredModel);
            return new AiLiveStatus("aimodel", "UP", true, configuredModel, modelReady);
        } catch (Exception e) {
            return new AiLiveStatus("aimodel", "UP", false, configuredModel, false);
        }
    }

    static List<String> parseOllamaModelNames(String json, ObjectMapper objectMapper) {
        List<String> names = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return names;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode models = root.get("models");
            if (models == null || !models.isArray()) {
                return names;
            }
            for (JsonNode model : models) {
                JsonNode nameNode = model.get("name");
                if (nameNode == null || !nameNode.isTextual()) {
                    continue;
                }
                String value = nameNode.asText().trim();
                if (!value.isEmpty()) {
                    names.add(value);
                }
            }
        } catch (Exception ignored) {
            // treat as no models
        }
        return names;
    }

    static boolean modelIsListed(List<String> names, String wanted) {
        if (wanted == null || wanted.isBlank() || names.isEmpty()) {
            return false;
        }
        if (names.contains(wanted)) {
            return true;
        }
        int colon = wanted.indexOf(':');
        String base = colon > 0 ? wanted.substring(0, colon) : wanted;
        for (String n : names) {
            if (base.equals(n) || (n != null && n.startsWith(base + ":"))) {
                return true;
            }
        }
        return false;
    }
}
