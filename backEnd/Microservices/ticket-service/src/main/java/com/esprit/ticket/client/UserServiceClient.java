package com.esprit.ticket.client;

import com.esprit.ticket.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.user-service.base-url}")
    private String userServiceBaseUrl;

    /**
     * Resolves numeric user id from user-service. Forwards the incoming {@code Authorization} header so calls
     * routed through the API gateway behave like the browser (some stacks require a bearer for user lookups).
     */
    public Long resolveUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "Cannot resolve user identity (missing email)");
        }
        String base = userServiceBaseUrl.replaceAll("/$", "");
        String url = base + "/email/" + UriUtils.encodePathSegment(email, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        copyAuthorizationFromCurrentRequest(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("id")) {
                throw new EntityNotFoundException("User not found for email: " + email);
            }
            Object idObj = body.get("id");
            if (idObj instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(idObj));
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            if (code == 404) {
                throw new ResponseStatusException(NOT_FOUND,
                        "Your account was not found in the user directory. Try signing out and signing in again.");
            }
            throw new ResponseStatusException(BAD_GATEWAY, "User service error (HTTP " + code + ")");
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "User service unavailable");
        }
    }

    private static void copyAuthorizationFromCurrentRequest(HttpHeaders out) {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.hasText(auth)) {
                    out.set(HttpHeaders.AUTHORIZATION, auth);
                }
            }
        } catch (Exception ignored) {
            // non-request thread or missing context — proceed without Authorization
        }
    }
}
