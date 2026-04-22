package com.esprit.user.dto;

import com.esprit.user.entity.Role;

import java.time.format.DateTimeFormatter;

/**
 * Safe DTO for API responses. All fields are simple types (no LocalDateTime)
 * so Jackson serialization cannot fail.
 */
public class UserResponse {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private String phone;
    private String avatarUrl;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;

    public static UserResponse fromEntity(com.esprit.user.entity.User u) {
        if (u == null) return null;
        UserResponse response = new UserResponse();
        response.setId(u.getId());
        response.setEmail(u.getEmail());
        response.setFirstName(u.getFirstName());
        response.setLastName(u.getLastName());
        response.setRole(u.getRole());
        response.setPhone(u.getPhone() != null ? u.getPhone() : "");
        response.setAvatarUrl(u.getAvatarUrl() != null ? u.getAvatarUrl() : "");
        response.setIsActive(u.getIsActive() != null ? u.getIsActive() : true);
        response.setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().format(ISO) : "");
        response.setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().format(ISO) : "");
        return response;
    }

    public UserResponse() {
    }

    public UserResponse(Long id, String email, String firstName, String lastName, Role role, String phone, String avatarUrl, Boolean isActive, String createdAt, String updatedAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
