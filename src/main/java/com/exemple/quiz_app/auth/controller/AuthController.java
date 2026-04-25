// auth/controller/AuthController.java
package com.exemple.quiz_app.auth.controller;

import com.exemple.quiz_app.auth.dto.*;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.service.QuizAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private QuizAdminService quizAdminService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        if (response.getMessage() != null && response.getMessage().contains("Email déjà utilisé")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        if (response.getMessage() != null && response.getMessage().contains("incorrect")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        AuthResponse response = authService.getCurrentUserInfo();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> updateProfile(
            @PathVariable BigInteger id,
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.updateProfile(id, request);
        if (response.getMessage() != null && response.getMessage().contains("refusé")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/user/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> changePassword(
            @PathVariable BigInteger id,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthResponse response = authService.changePassword(id, request);
        return ResponseEntity.ok(response);
    }

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
    public ResponseEntity<AuthResponse> getUserById(@PathVariable BigInteger id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @PutMapping("/promote/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> promoteToTeacher(@PathVariable BigInteger userId) {
        AuthResponse response = authService.promoteToTeacher(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> deleteUser(@PathVariable BigInteger id) {
        AuthResponse response = authService.deleteUser(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/quizzes/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Quiz>> getAllExpiredQuizzes() {
        return ResponseEntity.ok(quizAdminService.getAllExpiredQuizzes());
    }

    @GetMapping("/admin/quizzes/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Quiz>> getAllDeletedQuizzes() {
        return ResponseEntity.ok(quizAdminService.getAllDeletedQuizzes());
    }

    @DeleteMapping("/admin/quiz/{quizId}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> permanentlyDeleteQuiz(@PathVariable Long quizId) {
        try {
            quizAdminService.permanentlyDeleteQuiz(quizId);
            return ResponseEntity.ok(Map.of("message", "Quiz supprime definitivement avec succes"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/admin/quiz/{quizId}/soft-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> softDeleteQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizAdminService.softDeleteQuiz(quizId);
            return ResponseEntity.ok(Map.of(
                    "message", "Quiz marque comme supprime",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/admin/quiz/{quizId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> restoreQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizAdminService.restoreQuiz(quizId);
            return ResponseEntity.ok(Map.of(
                    "message", "Quiz restaure avec succes",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/quiz/{quizId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> blockQuiz(@PathVariable Long quizId) {
        try {
            Quiz quiz = quizAdminService.blockQuiz(quizId);
            return ResponseEntity.ok(Map.of(
                    "message", "Quiz bloque avec succes",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/admin/quiz/{quizId}/extend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> extendQuizExpiration(
            @PathVariable Long quizId,
            @RequestBody Map<String, String> request) {
        try {
            String newExpirationDate = request.get("newExpirationDate");
            Quiz quiz = quizAdminService.extendQuizExpiration(quizId, newExpirationDate);
            return ResponseEntity.ok(Map.of(
                    "message", "Date d'expiration prolongee",
                    "quiz", quiz
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/quizzes/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getQuizStatistics() {
        return ResponseEntity.ok(quizAdminService.getQuizStatistics());
    }
}