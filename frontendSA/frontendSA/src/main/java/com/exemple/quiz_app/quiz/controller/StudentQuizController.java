package com.exemple.quiz_app.quiz.controller;

import com.exemple.quiz_app.question.dto.QuestionDto;
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

    /**
     * 1. Quizzes disponibles pour l'étudiant
     */
    @GetMapping("/quizzes/available")
    public ResponseEntity<List<QuizForStudentDto>> getAvailableQuizzes() {
        return ResponseEntity.ok(studentQuizService.getAvailableQuizzes());
    }

    /**
     * 2. Historique des quizzes complétés
     */
    @GetMapping("/quizzes/history")
    public ResponseEntity<List<QuizForStudentDto>> getQuizHistory() {
        return ResponseEntity.ok(studentQuizService.getQuizHistory());
    }

    /**
     * 3. Détails d'un quiz
     */
    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<QuizForStudentDto> getQuizDetails(@PathVariable Long quizId) {
        return ResponseEntity.ok(studentQuizService.getQuizDetails(quizId));
    }

    /**
     * 4. Récupérer les questions d'un quiz (sans réponses correctes)


    /**
     * 5. Vérifier si l'étudiant peut participer (retourne un Map avec détails)
     * 🔥 CORRECTION : retourne Map<String, Object> au lieu de Boolean
     */
    @GetMapping("/quizzes/{quizId}/can-participate")
    public ResponseEntity<Map<String, Object>> canParticipate(@PathVariable Long quizId) {
        return ResponseEntity.ok(studentQuizService.canParticipate(quizId));
    }

    /**
     * 6. 🔥 Démarrer un quiz (créer une session avec timer)
     */
    @PostMapping("/quizzes/{quizId}/start")
    public ResponseEntity<Map<String, Object>> startQuiz(@PathVariable Long quizId) {
        studentQuizService.startQuiz(quizId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Quiz démarré avec succès");
        response.put("quizId", quizId);
        response.put("status", "STARTED");
        return ResponseEntity.ok(response);
    }

    /**
     * 7. 🔥 Vérifier le temps restant (appel périodique)
     */
    @GetMapping("/quizzes/{quizId}/time-remaining")
    public ResponseEntity<Map<String, Object>> getRemainingTime(@PathVariable Long quizId) {
        return ResponseEntity.ok(studentQuizService.getRemainingTime(quizId));
    }

    /**
     * 8. 🔥 Récupérer le temps restant en secondes
     */
    @GetMapping("/quizzes/{quizId}/remaining-seconds")
    public ResponseEntity<Map<String, Long>> getRemainingSeconds(@PathVariable Long quizId) {
        Long remainingSeconds = studentQuizService.getRemainingSecondsForQuiz(quizId);
        Map<String, Long> response = new HashMap<>();
        response.put("remainingSeconds", remainingSeconds);
        return ResponseEntity.ok(response);
    }
}