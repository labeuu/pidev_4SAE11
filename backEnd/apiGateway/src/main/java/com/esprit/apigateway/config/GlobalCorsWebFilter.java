package com.esprit.apigateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Adds CORS headers to every response. Handles OPTIONS preflight here.
 * Wraps the response so CORS headers are still present after the gateway proxies
 * the downstream response (which would otherwise overwrite headers and cause status 0 in browser).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalCorsWebFilter implements WebFilter {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        if (origin == null || !ALLOWED_ORIGINS.contains(origin)) {
            return chain.filter(exchange);
        }

        // Handle preflight: respond immediately with 204
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization, Content-Type, Accept, Origin, X-Requested-With, X-AI-Backend");
            response.setStatusCode(HttpStatus.NO_CONTENT);
            return response.setComplete();
        }

        // For GET/POST etc.: wrap response so CORS is always set on the actual headers
        // (gateway overwrites headers with downstream response; browser then blocks due to missing CORS -> status 0)
        final String allowedOrigin = origin;
        ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = super.getHeaders();
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                return headers;
            }
        };
        return chain.filter(exchange.mutate().response(decorated).build());
    }
}
