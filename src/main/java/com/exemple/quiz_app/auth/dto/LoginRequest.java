package com.exemple.quiz_app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank(message="Email doit etre fourni ")
    @Email(message="Format email invalide")
    private String Email;
    @NotBlank(message="le mot de passe doit etre fourni")
    @Size(min=8,message="le mot de passe entre doit au moins contenir 8 caracteres")
    private String password;
    public LoginRequest(){}
    public LoginRequest(String email,String passwd){
        this.Email=email;
        this.password=passwd;
    }
    public String getEmail() { return Email; }
    public void setEmail(String email) { Email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
