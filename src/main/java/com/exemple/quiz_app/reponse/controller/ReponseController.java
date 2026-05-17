package com.exemple.quiz_app.reponse.controller;

import com.exemple.quiz_app.reponse.dto.ReponseDetailDto;
import com.exemple.quiz_app.reponse.dto.ReponseRequestDto;
import com.exemple.quiz_app.reponse.dto.QuizSubmissionResponseDto;
import com.exemple.quiz_app.reponse.service.ReponseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reponses")
@RequiredArgsConstructor
public class ReponseController {

    private final ReponseService reponseService;

    /**
     * 1. Sauvegarder une réponse (SANS RETOUR - silence total)
     * Appelé à chaque clique sur "Suivant"
     * 🔥 Retourne 204 No Content - rien n'est affiché côté frontend
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Void> saveReponse(@Valid @RequestBody ReponseRequestDto request) {
        reponseService.saveOrUpdateReponse(request);
        return ResponseEntity.noContent().build();  // 🔥 204 - AUCUNE DONNÉE RETOURNÉE
    }

    /**
     * 2. Soumettre le quiz (SANS BODY) → score + feedback IA
     * Appelé au clique sur "Soumettre"
     */
    @PostMapping("/quiz/{quizId}/submit")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<QuizSubmissionResponseDto> submitQuiz(@PathVariable Long quizId) {
        QuizSubmissionResponseDto result = reponseService.submitQuizAndGetResult(quizId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 3. Voir les corrections (bouton "Voir corrections")
     * Appelé après soumission
     */
    @GetMapping("/quiz/{quizId}/corrections")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<List<ReponseDetailDto>> getCorrectionsDetails(@PathVariable Long quizId) {
        List<ReponseDetailDto> corrections = reponseService.getCorrectionsDetails(quizId);
        return ResponseEntity.ok(corrections);
    }
}