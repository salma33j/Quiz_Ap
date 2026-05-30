package com.exemple.quiz_app.question.controller;

import com.exemple.quiz_app.question.dto.QuestionRequestDto;
import com.exemple.quiz_app.question.dto.QuestionResponseDto;
import com.exemple.quiz_app.question.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('ENSEIGNANT') OR hasRole('ADMIN')")  // 🔥 Bloque tout le contrôleur
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * Ajouter une question à un quiz
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @PostMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<QuestionResponseDto> addQuestion(
            @PathVariable Long quizId,
            @Valid @RequestBody QuestionRequestDto request) {
        return ResponseEntity.ok(questionService.addQuestion(quizId, request));
    }

    /**
     * Ajouter plusieurs questions
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @PostMapping("/quizzes/{quizId}/questions/batch")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<QuestionResponseDto>> addMultipleQuestions(
            @PathVariable Long quizId,
            @Valid @RequestBody List<QuestionRequestDto> requests) {
        return ResponseEntity.ok(questionService.addMultipleQuestions(quizId, requests));
    }

    /**
     * Modifier une question
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<QuestionResponseDto> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequestDto request) {
        return ResponseEntity.ok(questionService.updateQuestion(questionId, request));
    }

    /**
     * Supprimer une question
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Question supprimee avec succes");
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer toutes les questions d'un quiz (avec réponses)
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @GetMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<QuestionResponseDto>> getQuestionsByQuiz(@PathVariable Long quizId) {
        return ResponseEntity.ok(questionService.getQuestionsByQuizForTeacher(quizId));
    }

    /**
     * Générer des questions par IA
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @PostMapping("/ai/generate")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<?> generateQuestionsByIA(
            @RequestParam String theme,
            @RequestParam int numberOfQuestions,
            @RequestParam(defaultValue = "MOYEN") String difficulty) {
        return ResponseEntity.ok(questionService.generateQuestionsByIA(theme, numberOfQuestions, difficulty));
    }

    /**
     * Sauvegarder les questions générées par IA
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @PostMapping("/quizzes/{quizId}/questions/ai/save")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<QuestionResponseDto>> saveGeneratedQuestions(
            @PathVariable Long quizId,
            @RequestBody List<QuestionRequestDto> validatedQuestions) {
        return ResponseEntity.ok(questionService.saveGeneratedQuestions(quizId, validatedQuestions));
    }

    /**
     * Récupérer une question spécifique (pour enseignant)
     * 🔐 Seul ENSEIGNANT ou ADMIN
     */
    @GetMapping("/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<QuestionResponseDto> getQuestionByIdForTeacher(@PathVariable Long questionId) {
        return ResponseEntity.ok(questionService.getQuestionByIdForTeacher(questionId));
    }

    // Nouveau endpoint dans QuestionController.java
    @PostMapping("/quizzes/{quizId}/questions/ai/generate-and-save")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<QuestionResponseDto>> generateAndSave(
            @PathVariable Long quizId,
            @RequestParam String theme,
            @RequestParam int numberOfQuestions,
            @RequestParam(defaultValue = "MOYEN") String difficulty) {
        return ResponseEntity.ok(
                questionService.generateAndSaveQuestions(quizId, theme, numberOfQuestions, difficulty)
        );
    }
}
