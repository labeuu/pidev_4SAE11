package com.esprit.user.dto;

import com.esprit.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request body for partial user update (PUT). All fields are optional.
 */
@Schema(description = "Request body for partial user update; all fields optional")
public class UserUpdateRequest {

    @Email
    @Schema(description = "User email address (optional)")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "New password (optional)")
    private String password;

    @Schema(description = "First name (optional)")
    private String firstName;

    @Schema(description = "Last name (optional)")
    private String lastName;

    @Schema(description = "User role (optional)")
    private Role role;

    @Schema(description = "Phone number (optional)")
    private String phone;

    @Schema(description = "Avatar URL (optional)")
    private String avatarUrl;

    @Schema(description = "Whether the account is active (optional)")
    private Boolean isActive;

    public UserUpdateRequest() {
    }

    public UserUpdateRequest(String email, String password, String firstName, String lastName, Role role, String phone, String avatarUrl, Boolean isActive) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.isActive = isActive;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
