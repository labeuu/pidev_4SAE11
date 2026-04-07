package com.esprit.ticket.client;

import com.esprit.ticket.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.user-service.base-url}")
    private String userServiceBaseUrl;

    public Long resolveUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot resolve user identity (missing email)");
        }
        String base = userServiceBaseUrl.replaceAll("/$", "");
        String url = base + "/email/" + UriUtils.encodePathSegment(email, StandardCharsets.UTF_8);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(url, Map.class);
            if (res == null || !res.containsKey("id")) {
                throw new EntityNotFoundException("User not found for email: " + email);
            }
            Object idObj = res.get("id");
            if (idObj instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(idObj));
        } catch (EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "User service unavailable");
        }
    }
}

