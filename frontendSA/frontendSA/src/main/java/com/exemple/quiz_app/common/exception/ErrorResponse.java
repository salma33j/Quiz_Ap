package com.exemple.quiz_app.common.exception;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private String details;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String message, String details) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
        this.code = getCodeFromStatus(status);
    }

    public ErrorResponse(int status, String code, String message, String details) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    private String getCodeFromStatus(int status) {
        switch (status) {
            case 400: return "BAD_REQUEST";
            case 401: return "UNAUTHORIZED";
            case 403: return "FORBIDDEN";
            case 404: return "NOT_FOUND";
            case 409: return "CONFLICT";
            case 500: return "INTERNAL_ERROR";
            default: return "ERROR";
        }
    }

    public static ErrorResponse notFound(String message) {
        return new ErrorResponse(404, message, "La ressource demandee n'existe pas");
    }

    public static ErrorResponse badRequest(String message) {
        return new ErrorResponse(400, message, "Requete invalide");
    }

    public static ErrorResponse unauthorized(String message) {
        return new ErrorResponse(401, message, "Authentification requise");
    }

    public static ErrorResponse forbidden(String message) {
        return new ErrorResponse(403, message, "Acces refuse");
    }

    public static ErrorResponse conflict(String message) {
        return new ErrorResponse(409, message, "Conflit avec les donnees existantes");
    }

    public static ErrorResponse internalError(String message) {
        return new ErrorResponse(500, message, "Erreur interne du serveur");
    }
}