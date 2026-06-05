package com.exemple.quiz_app.quiz.controller;

import com.exemple.quiz_app.quiz.dto.QuizForStudentDto;
import com.exemple.quiz_app.quiz.service.StudentQuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    @PostMapping("/quizzes/{quizId}/start")
    public ResponseEntity<Map<String, Object>> startQuiz(@PathVariable Long quizId) {
        studentQuizService.startQuiz(quizId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Quiz démarré avec succès");
        response.put("quizId", quizId);
        response.put("status", "STARTED");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/quizzes/{quizId}/time-remaining")
    public ResponseEntity<Map<String, Object>> getRemainingTime(@PathVariable Long quizId) {
        return ResponseEntity.ok(studentQuizService.getRemainingTime(quizId));
    }

    @GetMapping("/quizzes/{quizId}/remaining-seconds")
    public ResponseEntity<Map<String, Long>> getRemainingSeconds(@PathVariable Long quizId) {
        Long remainingSeconds = studentQuizService.getRemainingSecondsForQuiz(quizId);
        Map<String, Long> response = new HashMap<>();
        response.put("remainingSeconds", remainingSeconds);
        return ResponseEntity.ok(response);
    }
}
