package com.esprit.user.service;

import com.esprit.user.client.KeycloakAuthClient;
import com.esprit.user.dto.UserRequest;
import com.esprit.user.dto.UserUpdateRequest;
import com.esprit.user.entity.Role;
import com.esprit.user.entity.User;
import com.esprit.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for User operations in the Gestion User microservice.
 * Syncs delete/update with Keycloak via keycloak-auth service when configured.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAuthClient keycloakAuthClient;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, KeycloakAuthClient keycloakAuthClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakAuthClient = keycloakAuthClient;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Transactional
    public User create(UserRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        var existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        String passwordHash = request.getPassword() != null && !request.getPassword().isBlank()
                ? passwordEncoder.encode(request.getPassword())
                : passwordEncoder.encode("changeme");
        User user = new User();
        user.setEmail(email.trim());
        user.setPasswordHash(passwordHash);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, UserUpdateRequest request) {
        User user = findById(id);
        String oldEmail = user.getEmail();
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists with email: " + request.getEmail());
        }
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        User saved = userRepository.save(user);
        keycloakAuthClient.updateUserByEmail(oldEmail, saved.getFirstName(), saved.getLastName(),
            saved.getEmail(), saved.getRole() != null ? saved.getRole().name() : null);
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            return;
        }
        User user = findById(id);
        String email = user.getEmail();
        userRepository.deleteById(id);
        keycloakAuthClient.deleteUserByEmail(email);
    }
}
