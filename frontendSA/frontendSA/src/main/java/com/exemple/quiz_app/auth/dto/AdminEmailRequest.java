package com.exemple.quiz_app.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminEmailRequest {

    @NotBlank(message = "Le destinataire est obligatoire")
    private String target;

    @NotBlank(message = "L'objet est obligatoire")
    private String subject;

    @NotBlank(message = "Le message est obligatoire")
    private String message;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
