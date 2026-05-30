package com.exemple.quiz_app.quiz.controller;

import com.exemple.quiz_app.quiz.dto.QuizForStudentDto;
import com.exemple.quiz_app.quiz.dto.QuizReponse;
import com.exemple.quiz_app.quiz.dto.QuizRequest;
import com.exemple.quiz_app.quiz.dto.StudentListDto;
import com.exemple.quiz_app.quiz.service.QuizService;
import com.exemple.quiz_app.quiz.service.StudentQuizService;
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
@PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private StudentQuizService studentQuizService;

    @PostMapping("/quizzes")
    public ResponseEntity<QuizReponse> createQuiz(@Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.createQuiz(request));
    }

    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizReponse>> getMyQuizzes() {
        return ResponseEntity.ok(quizService.getMyQuizzes());
    }

    @GetMapping("/quizzes/{id}")
    public ResponseEntity<QuizReponse> getQuizById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PutMapping("/quizzes/{id}")
    public ResponseEntity<QuizReponse> updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.updateQuiz(id, request));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<Map<String, String>> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(Map.of("message", "Quiz supprime"));
    }

    @PostMapping("/quizzes/{id}/publish")
    public ResponseEntity<QuizReponse> publishQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.publishQuiz(id));
    }

    @PostMapping("/quizzes/{id}/students")
    public ResponseEntity<Map<String, Object>> addStudents(@PathVariable Long id, @RequestBody StudentListDto students) {
        int added = quizService.addAllowedStudents(id, students);
        return ResponseEntity.ok(Map.of("message", added + " etudiant(s) ajoute(s)", "addedCount", added));
    }

    @GetMapping("/quizzes/{id}/students")
    public ResponseEntity<List<StudentListDto.StudentInfo>> getStudents(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getAllowedStudents(id));
    }

    @DeleteMapping("/quizzes/{id}/students/{studentId}")
    public ResponseEntity<Map<String, String>> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        quizService.removeAllowedStudent(id, studentId);
        return ResponseEntity.ok(Map.of("message", "Etudiant retire"));
    }

    @GetMapping("/quizzes/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizStatus(id));
    }
}