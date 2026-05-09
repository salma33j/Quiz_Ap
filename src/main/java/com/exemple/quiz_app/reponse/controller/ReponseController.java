package com.exemple.quiz_app.reponse.controller;
import com.exemple.quiz_app.reponse.dto.ReponseRequestDto;
import com.exemple.quiz_app.reponse.dto.ReponseDto;
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
     * Enregistrer une réponse (pour une question spécifique)
     * Endpoint: POST /api/reponses
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<ReponseDto> saveReponse(@Valid @RequestBody ReponseRequestDto request) {
        ReponseDto response = reponseService.saveOrUpdateReponse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    /**
     * Enregistrer plusieurs réponses (soumission complète du quiz)
     * Endpoint: POST /api/reponses/batch
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<List<ReponseDto>> saveAllReponses(@Valid @RequestBody List<ReponseRequestDto> requests) {
        List<ReponseDto> responses = reponseService.saveAllReponses(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
    /**
     * Récupérer toutes les réponses de l'étudiant connecté pour un quiz
     * Endpoint: GET /api/reponses/quiz/{quizId}/my-reponses
     */
    @GetMapping("/quiz/{quizId}/my-reponses")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<List<ReponseDto>> getMyReponses(@PathVariable Long quizId) {
        List<ReponseDto> reponses = reponseService.getReponsesByStudentAndQuiz(quizId);
        return ResponseEntity.ok(reponses);
    }
    /**
     * Récupérer toutes les réponses pour un quiz (pour enseignant)
     * Endpoint: GET /api/reponses/quiz/{quizId}
     */
    @GetMapping("/quiz/{quizId}")
    @PreAuthorize("hasAnyRole('ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<List<ReponseDto>> getReponsesByQuiz(@PathVariable Long quizId) {
        List<ReponseDto> reponses = reponseService.getReponsesByQuiz(quizId);
        return ResponseEntity.ok(reponses);
    }
    /**
     * Vérifier si le quiz est complété par l'étudiant connecté
     * Endpoint: GET /api/reponses/quiz/{quizId}/completed
     */
    @GetMapping("/quiz/{quizId}/completed")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Boolean> isQuizCompleted(@PathVariable Long quizId) {
        boolean isCompleted = reponseService.isQuizCompleted(quizId);
        return ResponseEntity.ok(isCompleted);
    }
    /**
     * Obtenir le score actuel de l'étudiant pour un quiz
     * Endpoint: GET /api/reponses/quiz/{quizId}/current-score
     */
    @GetMapping("/quiz/{quizId}/current-score")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Integer> getCurrentScore(@PathVariable Long quizId) {
        Integer score = reponseService.getCurrentScore(quizId);
        return ResponseEntity.ok(score);
    }
    /**
     * Vérifier si l'étudiant est autorisé à participer au quiz
     * Endpoint: GET /api/reponses/quiz/{quizId}/authorized
     */
    @GetMapping("/quiz/{quizId}/authorized")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENSEIGNANT', 'ADMIN')")
    public ResponseEntity<Boolean> isAuthorizedForQuiz(@PathVariable Long quizId) {
        boolean isAuthorized = reponseService.isStudentAuthorizedForQuiz(quizId);
        return ResponseEntity.ok(isAuthorized);
    }
    /**
     * Supprimer toutes les réponses de l'étudiant pour un quiz (permettre de recommencer)
     * Endpoint: DELETE /api/reponses/quiz/{quizId}
     */
    @DeleteMapping("/quiz/{quizId}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMIN')")
    public ResponseEntity<Void> deleteMyReponses(@PathVariable Long quizId) {
        reponseService.deleteReponsesByStudentAndQuiz(quizId);
        return ResponseEntity.noContent().build();
    }
}
