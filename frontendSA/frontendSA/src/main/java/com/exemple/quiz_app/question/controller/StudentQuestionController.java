package com.exemple.quiz_app.question.controller;

import com.exemple.quiz_app.question.dto.QuestionDto;
import com.exemple.quiz_app.question.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
public class StudentQuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * Étudiant : récupérer toutes les questions d'un quiz (SANS réponses correctes)
     * 🔐 Seul ÉTUDIANT (vérifié par @PreAuthorize)
     */
    @GetMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<List<QuestionDto>> getQuestionsForStudent(@PathVariable Long quizId) {
        return ResponseEntity.ok(questionService.getQuestionsByQuizForStudent(quizId));
    }
}