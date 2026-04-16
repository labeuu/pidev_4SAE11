package com.esprit.keycloak.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for updating a user in Keycloak (internal API)")
public class KeycloakUserUpdateRequest {

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "New email (username) - optional")
    private String email;

    @Schema(description = "Realm role: CLIENT, FREELANCER, or ADMIN - optional")
    private String role;

    public KeycloakUserUpdateRequest() {
    }

    public KeycloakUserUpdateRequest(String firstName, String lastName, String email, String role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
