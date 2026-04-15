package com.esprit.aimodel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OllamaRestClientConfig {

    @Bean
    RestClient ollamaTagsRestClient(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            AImodelProperties statusProps) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(statusProps.connectTimeoutMs());
        factory.setReadTimeout(statusProps.readTimeoutMs());

        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
