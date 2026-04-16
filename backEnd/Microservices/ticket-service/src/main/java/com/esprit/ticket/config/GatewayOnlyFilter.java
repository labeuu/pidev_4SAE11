package com.esprit.ticket.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GatewayOnlyFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!"true".equals(request.getHeader("X-Internal-Gateway"))) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Direct access to ticket service is not allowed");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
