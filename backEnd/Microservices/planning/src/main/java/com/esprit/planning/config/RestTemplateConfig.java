package com.esprit.planning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    // Performs rest template.
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
