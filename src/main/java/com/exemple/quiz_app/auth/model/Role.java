package com.exemple.quiz_app.auth.model;
public enum Role {
      ETUDIANT,
      ENSEIGNANT;
    // Méthode utilitaire pour vérifier le rôle
    public boolean isEtudiant() {
        return this == ETUDIANT;
    }
    public boolean isEnseignant() {
        return this == ENSEIGNANT;
    }
    // Convertir une String en Role (utile pour les requêtes)
    public static Role fromString(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ETUDIANT; // Valeur par défaut
        }
    }
}







