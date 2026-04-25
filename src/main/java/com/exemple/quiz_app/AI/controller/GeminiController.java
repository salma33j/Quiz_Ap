package com.exemple.quiz_app.AI.controller;

import com.exemple.quiz_app.AI.dto.FeedbackRequestDto;
import com.exemple.quiz_app.AI.dto.FeedbackResponseDto;
import com.exemple.quiz_app.AI.dto.QuizGenerationRequestDto;
import com.exemple.quiz_app.AI.dto.QuizGenerationResponseDto;
import com.exemple.quiz_app.AI.service.AiFeedbackService;
import com.exemple.quiz_app.AI.service.AiQuizGenerationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class GeminiController{

    @Autowired
    private AiQuizGenerationService aiQuizGenerationService;

    @Autowired
    private AiFeedbackService aiFeedbackService;

    // ========== ENSEIGNANT : GÉNÉRATION QUIZ ==========

    @PostMapping("/generate-quiz")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<QuizGenerationResponseDto> generateQuiz(@Valid @RequestBody QuizGenerationRequestDto request) {
        QuizGenerationResponseDto response = aiQuizGenerationService.generateQuizContent(request);
        return ResponseEntity.ok(response);
    }

    // ========== ÉTUDIANT : FEEDBACK ==========

    @PostMapping("/feedback")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<FeedbackResponseDto> getFeedback(@Valid @RequestBody FeedbackRequestDto request) {
        FeedbackResponseDto response = aiFeedbackService.generateFeedback(request);
        return ResponseEntity.ok(response);
    }
}