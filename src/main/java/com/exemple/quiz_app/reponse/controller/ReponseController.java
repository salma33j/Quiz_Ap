package com.exemple.quiz_app.reponse.controller;

import com.exemple.quiz_app.reponse.dto.ReponseDetailDto;
import com.exemple.quiz_app.reponse.dto.ReponseDto;
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
     * 1. Sauvegarder/mettre à jour une réponse (pour suivant/précédent)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<ReponseDto> saveReponse(@Valid @RequestBody ReponseRequestDto request) {
        ReponseDto response = reponseService.saveOrUpdateReponse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. Soumettre le quiz → score + feedback IA
     */
    @PostMapping("/quiz/submit")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<QuizSubmissionResponseDto> submitQuiz(
            @Valid @RequestBody List<ReponseRequestDto> requests) {
        QuizSubmissionResponseDto result = reponseService.submitQuizAndGetResult(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 3. Voir les corrections (bouton "Voir corrections")
     */
    @GetMapping("/quiz/{quizId}/corrections")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<List<ReponseDetailDto>> getCorrectionsDetails(@PathVariable Long quizId) {
        List<ReponseDetailDto> corrections = reponseService.getCorrectionsDetails(quizId);
        return ResponseEntity.ok(corrections);
    }
}