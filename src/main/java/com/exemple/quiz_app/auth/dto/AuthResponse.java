package com.exemple.quiz_app.auth.dto;

import java.time.LocalDateTime;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String type;
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String fullName;
    private String message;
    private boolean success;
    private Long expiresIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ✅ NOUVEAU : indique si l'utilisateur doit changer son mot de passe
    private boolean mustChangePassword;

    // Constructeur par défaut
    public AuthResponse() {
        this.type = "Bearer";
        this.success = true;
        this.mustChangePassword = false;
    }

    public AuthResponse(String token, String username, String role, Long id) {
        this();
        this.token = token;
        this.username = username;
        this.role = role;
        this.userId = id;
    }

    public AuthResponse(String token, String username, String email, String role, Long id) {
        this(token, username, role, id);
        this.email = email;
    }

    public AuthResponse(String message, boolean success) {
        this();
        this.message = message;
        this.success = success;
    }

    public AuthResponse(String token, String refreshToken, String type, Long id,
                        String username, String email, String role, String fullName,
                        Long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.type = type;
        this.userId = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.expiresIn = expiresIn;
        this.success = true;
        this.mustChangePassword = false;
    }

    // Getters
    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getType() { return type; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public Long getExpiresIn() { return expiresIn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isMustChangePassword() { return mustChangePassword; }

    // Setters
    public void setToken(String token) { this.token = token; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setType(String type) { this.type = type; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setMessage(String message) { this.message = message; }
    public void setSuccess(boolean success) { this.success = success; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public static AuthResponse error(String message) {
        return new AuthResponse(message, false);
    }

    public static AuthResponse success(String token, String username, String role, Long id) {
        return new AuthResponse(token, username, role, id);
    }

    public static AuthResponse success(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='***'" +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", mustChangePassword=" + mustChangePassword +
                '}';
    }
}
