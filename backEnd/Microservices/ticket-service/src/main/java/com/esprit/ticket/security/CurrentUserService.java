package com.esprit.ticket.security;

import com.esprit.ticket.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserServiceClient userServiceClient;

    private Long cachedUserId;
    private String cachedEmail;

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public Long requireCurrentUserId() {
        if (cachedUserId != null) return cachedUserId;
        String email = requireEmailFromJwt();
        cachedEmail = email;
        cachedUserId = userServiceClient.resolveUserIdByEmail(email);
        return cachedUserId;
    }

    public String requireCurrentEmail() {
        if (cachedEmail != null) return cachedEmail;
        cachedEmail = requireEmailFromJwt();
        return cachedEmail;
    }

    private String requireEmailFromJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) return email;
            String preferred = jwt.getClaimAsString("preferred_username");
            if (preferred != null && !preferred.isBlank()) return preferred;
            throw new ResponseStatusException(FORBIDDEN, "JWT missing email");
        }
        throw new ResponseStatusException(UNAUTHORIZED, "Invalid authentication principal");
    }
}

