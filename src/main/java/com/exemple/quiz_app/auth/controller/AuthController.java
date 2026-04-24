// auth/controller/AuthController.java
package com.exemple.quiz_app.auth.controller;

import com.exemple.quiz_app.auth.dto.*;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.quiz.dto.QuizAdminActionDto;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.service.QuizAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

<<<<<<< HEAD
    @Autowired
    private AuthService authService;

    @Autowired
    private QuizAdminService quizAdminService;

    // ================= AUTHENTIFICATION (PUBLIC) =================

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        if (response.getMessage().contains("Email déjà utilisé")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        if (response.getMessage().contains("incorrect")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // ================= PROFIL UTILISATEUR (PROTÉGÉ) =================

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        AuthResponse response = authService.getCurrentUserInfo();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.updateProfile(id, request);
        if (response.getMessage().contains("refusé")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthResponse response = authService.changePassword(id, request);
        return ResponseEntity.ok(response);
    }

    // ================= ADMIN ONLY : GESTION DES UTILISATEURS =================

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(authService.getAllUsers());
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
    }

    @GetMapping("/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @PutMapping("/promote/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> promoteToTeacher(@PathVariable Long userId) {
        AuthResponse response = authService.promoteToTeacher(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> deleteUser(@PathVariable Long id) {
        AuthResponse response = authService.deleteUser(id);
        return ResponseEntity.ok(response);
    }

    // ================= ADMIN ONLY : GESTION DES QUIZ EXPIRÉS =================

    /**
     * ADMIN : Liste tous les quiz expirés (soft delete - marqués DELETED)
     */
    @GetMapping("/admin/quizzes/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Quiz>> getAllExpiredQuizzes() {
        return ResponseEntity.ok(quizAdminService.getAllExpiredQuizzes());
    }

    /**
     * ADMIN : Liste tous les quiz supprimés (soft delete)
     */
    @GetMapping("/admin/quizzes/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Quiz>> getAllDeletedQuizzes() {
        return ResponseEntity.ok(quizAdminService.getAllDeletedQuizzes());
    }

    /**
     * ADMIN : Supprimer définitivement un quiz expiré (HARD DELETE)
     * ⚠️ Attention : Supprime le quiz ET toutes les réponses associées
     */
    @DeleteMapping("/admin/quiz/{quizId}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> permanentlyDeleteQuiz(@PathVariable Long quizId) {
        try {
            quizAdminService.permanentlyDeleteQuiz(quizId);
            return ResponseEntity.ok(Map.of("message", "Quiz supprimé définitivement avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ADMIN : Soft delete d'un quiz (marquer comme supprimé sans effacer)
     */
    @PutMapping("/admin/quiz/{quizId}/soft-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> softDeleteQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizAdminService.softDeleteQuiz(quizId);
            return ResponseEntity.ok(Map.of(
                    "message", "Quiz marqué comme supprimé",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ADMIN : Restaurer un quiz soft-deleté
     */
    @PutMapping("/admin/quiz/{quizId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restoreQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizAdminService.restoreQuiz(quizId);
            return ResponseEntity.ok(Map.of(
                    "message", "Quiz restauré avec succès",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ADMIN : Bloquer un quiz (le rendre expiré immédiatement)
     */
    @PostMapping("/admin/quiz/{quizId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> blockQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizAdminService.blockQuiz(quizId);
            return ResponseEntity.ok(Map.of(
                    "message", "Quiz bloqué avec succès (expiré)",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ADMIN : Prolonger la date d'expiration d'un quiz
     */
    @PutMapping("/admin/quiz/{quizId}/extend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> extendQuizExpiration(
            @PathVariable Long quizId,
            @RequestBody Map<String, String> request) {
        try {
            String newExpirationDate = request.get("newExpirationDate");
            Quiz quiz = quizAdminService.extendQuizExpiration(quizId, newExpirationDate);
            return ResponseEntity.ok(Map.of(
                    "message", "Date d'expiration prolongée",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ADMIN : Statistiques des quiz (nombre total, expirés, supprimés)
     */
    @GetMapping("/admin/quizzes/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getQuizStatistics() {
        return ResponseEntity.ok(quizAdminService.getQuizStatistics());
    }
}
=======
}
>>>>>>> 0a126cdee69fe4d7d0be101497e73be7009d4831
