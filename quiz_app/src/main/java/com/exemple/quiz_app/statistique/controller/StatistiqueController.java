package com.exemple.quiz_app.statistique.controller;

import com.exemple.quiz_app.statistique.dto.StatistiqueDto;
import com.exemple.quiz_app.statistique.service.StatistiqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistiques")
public class StatistiqueController {

    @Autowired
    private StatistiqueService statistiqueService;

    // ========== ENSEIGNANT / ADMIN ==========

    /**
     * Obtenir les statistiques détaillées d'un quiz
     * GET /api/statistiques/quiz/{quizId}
     */
    @GetMapping("/quiz/{quizId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<StatistiqueDto> getQuizStatistics(@PathVariable Long quizId) {
        return ResponseEntity.ok(statistiqueService.getQuizStatistics(quizId));
    }

    /**
     * Obtenir les statistiques pour le dashboard enseignant (tous ses quiz)
     * GET /api/statistiques/teacher/dashboard
     */
    @GetMapping("/teacher/dashboard")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<StatistiqueDto>> getTeacherDashboardStats() {
        return ResponseEntity.ok(statistiqueService.getTeacherDashboardStats());
    }

    /**
     * Obtenir les statistiques de tous les quiz (admin)
     * GET /api/statistiques/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StatistiqueDto>> getAllQuizzesStatistics() {
        return ResponseEntity.ok(statistiqueService.getAllQuizzesStatistics());
    }

    /**
     * Obtenir les statistiques globales de l'application (admin)
     * GET /api/statistiques/admin/global
     */
    @GetMapping("/admin/global")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getGlobalStatistics() {
        return ResponseEntity.ok(statistiqueService.getGlobalStatistics());
    }

    // ========== ÉTUDIANT ==========

    /**
     * Obtenir les statistiques personnelles de l'étudiant
     * GET /api/statistiques/student/my-performance
     */
    @GetMapping("/student/my-performance")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<StatistiqueDto> getMyPerformance() {
        return ResponseEntity.ok(statistiqueService.getMyPerformance());
    }

    /**
     * Obtenir le classement de l'étudiant pour un quiz spécifique
     * GET /api/statistiques/student/ranking/{quizId}
     */
    @GetMapping("/student/ranking/{quizId}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<StatistiqueDto.StudentStatDto> getMyRanking(@PathVariable Long quizId) {
        return ResponseEntity.ok(statistiqueService.getMyRanking(quizId));
    }
}