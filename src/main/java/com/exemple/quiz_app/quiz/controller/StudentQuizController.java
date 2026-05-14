package com.exemple.quiz_app.quiz.controller;

import com.exemple.quiz_app.quiz.dto.QuizForStudentDto;
import com.exemple.quiz_app.quiz.service.StudentQuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
public class StudentQuizController {

    @Autowired
    private StudentQuizService studentQuizService;

    @GetMapping("/quizzes/available")
    public ResponseEntity<List<QuizForStudentDto>> getAvailableQuizzes() {
        return ResponseEntity.ok(studentQuizService.getAvailableQuizzes());
    }

    @GetMapping("/quizzes/history")
    public ResponseEntity<List<QuizForStudentDto>> getQuizHistory() {
        return ResponseEntity.ok(studentQuizService.getQuizHistory());
    }

    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<QuizForStudentDto> getQuizDetails(@PathVariable Long quizId) {
        return ResponseEntity.ok(studentQuizService.getQuizDetails(quizId));
    }

    @GetMapping("/quizzes/{quizId}/can-participate")
    public ResponseEntity<Map<String, Object>> canParticipate(@PathVariable Long quizId) {
        return ResponseEntity.ok(studentQuizService.canParticipate(quizId));
    }
}