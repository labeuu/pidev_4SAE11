package com.esprit.task.config;

import feign.Request;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AImodelFeignConfigurationTest {

    @Test
    void aimodelFeignRequestOptions_setsConnectAndReadTimeouts() {
        AImodelFeignConfiguration cfg = new AImodelFeignConfiguration();
        Request.Options opts = cfg.aimodelFeignRequestOptions();

        // Feign returns the raw duration in the unit passed to the constructor (seconds / hours).
        assertThat(opts.connectTimeout()).isEqualTo(30L);
        assertThat(opts.readTimeout()).isEqualTo(4L);
    }
}
