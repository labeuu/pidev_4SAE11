package com.esprit.aimodel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aimodel.status")
public record AImodelProperties(int connectTimeoutMs, int readTimeoutMs) {

    public AImodelProperties {
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 5000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 5000;
        }
    }
}
