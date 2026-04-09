package com.esprit.aimodel.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderStatusServiceModelListedTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void parseOllamaModelNamesExtractsModelIds() {
        String json = "{\"models\":[{\"name\":\"qwen3:8b\"},{\"name\":\"llama3.2\"}]}";
        assertThat(ProviderStatusService.parseOllamaModelNames(json, MAPPER))
                .containsExactlyInAnyOrder("qwen3:8b", "llama3.2");
    }

    @Test
    void modelIsListedMatchesConfiguredId() {
        List<String> names = ProviderStatusService.parseOllamaModelNames(
                "{\"models\":[{\"name\":\"qwen3:8b\"}]}", MAPPER);
        assertThat(ProviderStatusService.modelIsListed(names, "qwen3:8b")).isTrue();
    }

    @Test
    void modelIsListedPrefixBeforeColon() {
        assertThat(ProviderStatusService.modelIsListed(List.of("qwen3"), "qwen3:8b")).isTrue();
    }

    @Test
    void modelIsListedEmptyWanted() {
        assertThat(ProviderStatusService.modelIsListed(List.of("a"), "")).isFalse();
    }
}
