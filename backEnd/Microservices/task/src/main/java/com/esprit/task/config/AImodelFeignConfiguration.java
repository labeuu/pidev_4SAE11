package com.esprit.task.config;

import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.concurrent.TimeUnit;

/**
 * Long read/connect timeouts for Task → AImodel → Ollama. OpenFeign may ignore
 * {@code spring.cloud.openfeign.client.config.*} for some clients (e.g. with {@code contextId}),
 * so {@link Request.Options} is applied explicitly for the AIMODEL Feign client only.
 */
public class AImodelFeignConfiguration {

    private static final long CONNECT_SEC = 30L;
    private static final long READ_HOURS = 4L;

    @Bean
    public Request.Options aimodelFeignRequestOptions() {
        return new Request.Options(CONNECT_SEC, TimeUnit.SECONDS, READ_HOURS, TimeUnit.HOURS, true);
    }

    @Bean
    public RequestInterceptor aimodelInternalHeadersInterceptor() {
        return template -> {
            // AIMODEL enforces gateway-only access for internal calls too.
            template.header("X-Internal-Gateway", "true");

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getToken() != null) {
                template.header("Authorization", "Bearer " + jwtAuth.getToken().getTokenValue());
            }
        };
    }
}
