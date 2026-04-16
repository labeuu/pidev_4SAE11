package com.esprit.planning.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GatewayOnlyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator/health") || path.startsWith("/actuator/info")) {
            filterChain.doFilter(request, response);
            return;
        }

        String fromGateway = request.getHeader("X-Internal-Gateway");
        if (!"true".equals(fromGateway)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Direct access to planning service is not allowed");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

