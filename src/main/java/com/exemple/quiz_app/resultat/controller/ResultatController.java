package com.exemple.quiz_app.resultat.controller;

import com.exemple.quiz_app.resultat.dto.ResultatRequestDto;
import com.exemple.quiz_app.resultat.dto.ResultatDto;
import com.exemple.quiz_app.resultat.service.ResultatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/resultats")
@RequiredArgsConstructor
public class ResultatController {
    private final ResultatService resultatService;
    /**
     * Enregistrer ou mettre à jour un résultat
     * Endpoint: POST /api/resultats
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<ResultatDto> saveOrUpdateResultat(@Valid @RequestBody ResultatRequestDto resultatRequestDto) {
        ResultatDto resultat = resultatService.saveOrUpdateResultat(resultatRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultat);
    }
    /**
     * Obtenir le résultat de l'étudiant connecté pour un quiz
     * Endpoint: GET /api/resultats/quiz/{quizId}/my-resultat
     */
    @GetMapping("/quiz/{quizId}/my-resultat")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<ResultatDto> getMyResultat(@PathVariable Long quizId) {
        ResultatDto resultat = resultatService.getResultatByStudentAndQuiz(quizId);
        if (resultat == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resultat);
    }
    /**
     * Obtenir tous les résultats de l'étudiant connecté (historique)
     * Endpoint: GET /api/resultats/my-history
     */
    @GetMapping("/my-history")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<List<ResultatDto>> getMyHistory() {
        List<ResultatDto> resultats = resultatService.getResultatsByStudent();
        return ResponseEntity.ok(resultats);
    }
    /**
     * Obtenir tous les résultats pour un quiz (pour enseignant)
     * Endpoint: GET /api/resultats/quiz/{quizId}
     */
    @GetMapping("/quiz/{quizId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<ResultatDto>> getResultatsByQuiz(@PathVariable Long quizId) {
        List<ResultatDto> resultats = resultatService.getResultatsByQuiz(quizId);
        return ResponseEntity.ok(resultats);
    }
    /**
     * Obtenir les statistiques détaillées pour un quiz
     * Endpoint: GET /api/resultats/quiz/{quizId}/statistics
     */
    @GetMapping("/quiz/{quizId}/statistics")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<ResultatService.QuizStatisticsDto> getQuizStatistics(@PathVariable Long quizId) {
        ResultatService.QuizStatisticsDto statistics = resultatService.getQuizStatistics(quizId);
        return ResponseEntity.ok(statistics);
    }
    /**
     * Obtenir le classement des étudiants pour un quiz
     * Endpoint: GET /api/resultats/quiz/{quizId}/ranking
     */
    @GetMapping("/quiz/{quizId}/ranking")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<ResultatService.RankingDto>> getRanking(@PathVariable Long quizId) {
        // Cette méthode nécessite d'être exposée dans le service
        // Pour l'instant, on utilise getQuizStatistics qui contient le ranking
        ResultatService.QuizStatisticsDto statistics = resultatService.getQuizStatistics(quizId);
        return ResponseEntity.ok(statistics.getRanking());
    }
    /**
     * Générer un feedback IA pour un résultat
     * Endpoint: POST /api/resultats/{resultatId}/feedback-ia
     */
    @PostMapping("/{resultatId}/feedback-ia")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<ResultatDto> generateFeedback(
            @PathVariable Long resultatId,
            @RequestParam(defaultValue = "fr") String language) {
        ResultatDto resultat = resultatService.generateFeedbackForResultat(resultatId, language);
        return ResponseEntity.ok(resultat);
    }
    /**
     * Vérifier si l'étudiant connecté a complété le quiz
     * Endpoint: GET /api/resultats/quiz/{quizId}/has-completed
     */
    @GetMapping("/quiz/{quizId}/has-completed")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Boolean> hasCompletedQuiz(@PathVariable Long quizId) {
        boolean hasCompleted = resultatService.hasCompletedQuiz(quizId);
        return ResponseEntity.ok(hasCompleted);
    }
    /**
     * Supprimer le résultat de l'étudiant connecté pour un quiz
     * Endpoint: DELETE /api/resultats/quiz/{quizId}
     */
    @DeleteMapping("/quiz/{quizId}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Void> deleteMyResultat(@PathVariable Long quizId) {
        resultatService.deleteResultatByStudentAndQuiz(quizId);
        return ResponseEntity.noContent().build();
    }
}
