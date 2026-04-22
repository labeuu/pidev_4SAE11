package com.esprit.keycloak.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for token endpoint (password grant).
 */
public class TokenRequest {

    @NotBlank
    private String username;  // email

    @NotBlank
    private String password;

    private String grantType = "password";

    public TokenRequest() {
    }

    public TokenRequest(String username, String password, String grantType) {
        this.username = username;
        this.password = password;
        this.grantType = grantType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
}
