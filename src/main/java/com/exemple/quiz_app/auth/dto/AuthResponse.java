package com.exemple.quiz_app.auth.dto;

import java.math.BigInteger;

public class AuthResponse {
    private String token;
    private String refreshToken;
    private String type;
    private BigInteger userId;  //  Changé de Long à BigInteger
    private String username;
    private String email;
    private String role;
    private String fullName;
    private String message;
    private boolean success;
    private Long expiresIn;

    // Constructeur par défaut
    public AuthResponse() {
        this.type = "Bearer";
        this.success = true;
    }

    // Constructeur pour connexion réussie (minimum requis)
    public AuthResponse(String token, String username, String role, BigInteger id) {
        this();
        this.token = token;
        this.username = username;
        this.role = role;
        this.userId = id;
    }

    // Constructeur pour connexion réussie avec email
    public AuthResponse(String token, String username, String email, String role, BigInteger id) {
        this(token, username, role, id);
        this.email = email;
    }

    // Constructeur pour réponse d'erreur
    public AuthResponse(String message, boolean success) {
        this();
        this.message = message;
        this.success = success;
    }

    // Constructeur complet
    public AuthResponse(String token, String refreshToken, String type, BigInteger id,
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
    }

    // Getters
    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public String getType() { return type; }
    public BigInteger getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public Long getExpiresIn() { return expiresIn; }

    // Setters
    public void setToken(String token) { this.token = token; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setType(String type) { this.type = type; }
    public void setUserId(BigInteger userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setMessage(String message) { this.message = message; }
    public void setSuccess(boolean success) { this.success = success; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }

    // Méthode utilitaire pour créer une réponse d'erreur
    public static AuthResponse error(String message) {
        return new AuthResponse(message, false);
    }

    // Méthode utilitaire pour créer une réponse de succès simple
    public static AuthResponse success(String token, String username, String role, BigInteger id) {
        return new AuthResponse(token, username, role, id);
    }

    // Méthode utilitaire pour créer une réponse de succès sans token
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
                ", refreshToken='***'" +
                ", type='" + type + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", fullName='" + fullName + '\'' +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", expiresIn=" + expiresIn +
                '}';
    }

}