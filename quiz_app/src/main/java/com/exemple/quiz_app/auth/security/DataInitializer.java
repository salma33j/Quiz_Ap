package com.exemple.quiz_app.auth.security;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            System.out.println("✅ [DataInitializer] Données déjà présentes — aucune insertion.");
            return;
        }

        System.out.println("🚀 [DataInitializer] Insertion des données de démo...");

        // Constructeur : (firstName, lastName, email, password, role)
        userRepository.save(new User("Admin", "System",
                "akilsalma33@gmail.com", passwordEncoder.encode("admin123"), Role.ADMIN));

        userRepository.save(new User("Professeur", "Alpha",
                "prof.alpha@quizapp.com", passwordEncoder.encode("teacher123"), Role.ENSEIGNANT));

        userRepository.save(new User("Professeur", "Beta",
                "prof.beta@quizapp.com", passwordEncoder.encode("teacher123"), Role.ENSEIGNANT));

        userRepository.save(new User("Jean", "Dupont",
                "jean.dupont@etu.quizapp.com", passwordEncoder.encode("student123"), Role.ETUDIANT));

        userRepository.save(new User("Marie", "Martin",
                "marie.martin@etu.quizapp.com", passwordEncoder.encode("student123"), Role.ETUDIANT));

        userRepository.save(new User("Karim", "Benali",
                "karim.benali@etu.quizapp.com", passwordEncoder.encode("student123"), Role.ETUDIANT));

        System.out.println("✅ [DataInitializer] Données insérées avec succès !");
    }
}
