package com.exemple.quiz_app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Le nom doit etre fourni")
    private String firstName;

    @NotBlank(message = "Le prenom doit etre fourni")
    private String lastName;

    @NotBlank(message = "Email doit etre mentionne")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe doit etre fourni")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caracteres")
    private String password;

    private String role;

    private Long classId;

    private String cne;

    private String codeApoge;

    public RegisterRequest() {}

    public RegisterRequest(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }

    public String getCne() { return cne; }
    public void setCne(String cne) { this.cne = cne; }

    public String getCodeApoge() { return codeApoge; }
    public void setCodeApoge(String codeApoge) { this.codeApoge = codeApoge; }
}
