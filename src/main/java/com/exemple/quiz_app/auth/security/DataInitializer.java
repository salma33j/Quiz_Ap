package com.exemple.quiz_app.auth.security;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ✅ Ce composant s'exécute automatiquement au démarrage de Spring Boot.
 * Il insère les données de démo UNIQUEMENT si la base est vide.
 * Tu n'as jamais besoin d'ouvrir MySQL Workbench pour ça.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    // ✅ BCrypt — même encodeur que AuthService
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private String encode(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public void run(String... args) {

        // ✅ Ne rien faire si des données existent déjà
        if (userRepository.count() > 0) {
            System.out.println("✅ [DataInitializer] Données déjà présentes — aucune insertion.");
            return;
        }

        System.out.println("🚀 [DataInitializer] Insertion des données de démo...");

        // ==================== ADMIN ====================
        User admin = new User(
                "Admin System",
                "admin@quizapp.com",
                encode("admin123"),
                Role.ADMIN
        );
        userRepository.save(admin);

        // ==================== ENSEIGNANT 1 ====================
        User teacher1 = new User(
                "Professeur Alpha",
                "prof.alpha@quizapp.com",
                encode("teacher123"),
                Role.ENSEIGNANT
        );
        userRepository.save(teacher1);

        // ==================== ENSEIGNANT 2 ====================
        User teacher2 = new User(
                "Professeur Beta",
                "prof.beta@quizapp.com",
                encode("teacher123"),
                Role.ENSEIGNANT
        );
        userRepository.save(teacher2);

        // ==================== ÉTUDIANT 1 ====================
        User student1 = new User(
                "Étudiant Jean",
                "jean.dupont@etu.quizapp.com",
                encode("student123"),
                Role.ETUDIANT
        );
        userRepository.save(student1);

        // ==================== ÉTUDIANT 2 ====================
        User student2 = new User(
                "Étudiante Marie",
                "marie.martin@etu.quizapp.com",
                encode("student123"),
                Role.ETUDIANT
        );
        userRepository.save(student2);

        // ==================== ÉTUDIANT 3 ====================
        User student3 = new User(
                "Étudiant Karim",
                "karim.benali@etu.quizapp.com",
                encode("student123"),
                Role.ETUDIANT
        );
        userRepository.save(student3);

        System.out.println("[DataInitializer] Données insérées avec succès !");
        System.out.println("   Récapitulatif des comptes :");
        System.out.println("   ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("   │  ADMIN    : admin@quizapp.com          / admin123         │");
        System.out.println("   │  ENSEIGNANT : prof.alpha@quizapp.com   / teacher123       │");
        System.out.println("   │  ENSEIGNANT : prof.beta@quizapp.com    / teacher123       │");
        System.out.println("   │  S: jean.dupont@etu.quizapp.com / student123      │");
        System.out.println("   │   S: marie.martin@etu.quizapp.com / student123      │");
        System.out.println("   │  S: karim.benali@etu.quizapp.com / student123      │");
        System.out.println("   └─────────────────────────────────────────────────────────────┘");
    }
}