package com.exemple.quiz_app.auth.repository;

// auth/repository/UserRepository.java (version minimaliste)
package com.example.quiz_app.auth.repository;

import com.example.quiz_app.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Trouver un utilisateur par son email
     */
    Optional<User> findByEmail(String email);

    /**
     * Vérifier si un email existe déjà
     */
    boolean existsByEmail(String email);

    /**
     * Vérifier si un utilisateur existe par ID
     */
    boolean existsById(Long id);
}