package com.exemple.quiz_app.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message="Le nom doit etre fourni")
    private String nom;
    @NotBlank(message="Email doit etre mentionne")
    @Email(message="Format email invalide")
    private String email;
    @NotBlank(message="le mot de passe doit etre fourni")
    @Size(min=8,message="Le mot de passe doit contenir au moins 8 caracteres")
    private String motDePasse;
    public RegisterRequest(){}
    public RegisterRequest(String nom,String email,String motDepasse){
         this.nom=nom;
         this.email=email;
         this.motDePasse=motDepasse;
    }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse;}

}
