package com.esprit.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * User info returned from JWT or Keycloak userinfo endpoint.
 * Used by the User service and other microservices to identify the authenticated user.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserInfoResponse {

    private String sub;           // Keycloak user ID (UUID)
    private String email;
    private String firstName;
    private String lastName;
    private String preferredUsername;
    private List<String> roles;

    public UserInfoResponse() {
    }

    public UserInfoResponse(String sub, String email, String firstName, String lastName, String preferredUsername, List<String> roles) {
        this.sub = sub;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.preferredUsername = preferredUsername;
        this.roles = roles;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
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

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
