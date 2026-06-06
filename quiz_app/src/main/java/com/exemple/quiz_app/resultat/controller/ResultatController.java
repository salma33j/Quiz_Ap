// ======================= ResultatController.java =======================

package com.exemple.quiz_app.resultat.controller;

import com.exemple.quiz_app.resultat.dto.ResultatDto;
import com.exemple.quiz_app.resultat.dto.ResultatRequestDto;
import com.exemple.quiz_app.resultat.service.ResultatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resultats")
@RequiredArgsConstructor
public class ResultatController {

    private final ResultatService resultatService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<ResultatDto> saveOrUpdateResultat(
            @Valid @RequestBody ResultatRequestDto resultatRequestDto) {

        ResultatDto resultat = resultatService.saveOrUpdateResultat(resultatRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(resultat);
    }

    @GetMapping("/quiz/{quizId}/my-resultat")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<ResultatDto> getMyResultat(@PathVariable Long quizId) {

        ResultatDto resultat = resultatService.getResultatByStudentAndQuiz(quizId);

        if (resultat == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(resultat);
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<List<ResultatDto>> getMyHistory() {

        return ResponseEntity.ok(resultatService.getResultatsByStudent());
    }



    @GetMapping("/my-performance")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getMyPerformance() {
        return ResponseEntity.ok(resultatService.getMyPerformance());
    }


    @GetMapping("/quiz/{quizId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<ResultatDto>> getResultatsByQuiz(@PathVariable Long quizId) {

        return ResponseEntity.ok(resultatService.getResultatsByQuiz(quizId));
    }

    @GetMapping("/quiz/{quizId}/statistics")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<ResultatService.QuizStatisticsDto> getQuizStatistics(
            @PathVariable Long quizId) {

        return ResponseEntity.ok(resultatService.getQuizStatistics(quizId));
    }

    @GetMapping("/quiz/{quizId}/ranking")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<ResultatService.RankingDto>> getRanking(
            @PathVariable Long quizId) {

        return ResponseEntity.ok(resultatService.getRankingForCurrentUser(quizId));
    }

    // 🔥 FEEDBACK AI OPTIMISÉ
    @PostMapping("/{resultatId}/feedback-ia")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<ResultatDto> generateFeedback(
            @PathVariable Long resultatId,
            @RequestParam(defaultValue = "fr") String language) {

        ResultatDto resultat =
                resultatService.generateFeedbackForResultat(resultatId, language);

        return ResponseEntity.ok(resultat);
    }

    @GetMapping("/quiz/{quizId}/has-completed")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Boolean> hasCompletedQuiz(@PathVariable Long quizId) {

        return ResponseEntity.ok(
                resultatService.hasCompletedQuiz(quizId)
        );
    }

    @DeleteMapping("/quiz/{quizId}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Void> deleteMyResultat(@PathVariable Long quizId) {

        resultatService.deleteResultatByStudentAndQuiz(quizId);

        return ResponseEntity.noContent().build();
    }
}
