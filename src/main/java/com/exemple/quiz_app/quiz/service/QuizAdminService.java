package com.exemple.quiz_app.quiz.service;

// quiz/service/QuizAdminService.java


import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizAdminService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private ReponseRepository reponseRepository;

    @Autowired
    private AuthService authService;

    /**
     * ADMIN : Liste tous les quiz expirés (statut EXPIRED)
     */
    public List<Quiz> getAllExpiredQuizzes() {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }
        return quizRepository.findByStatus(Quiz.QuizStatus.EXPIRED);
    }

    /**
     * ADMIN : Liste tous les quiz supprimés (soft delete - status DELETED)
     */
    public List<Quiz> getAllDeletedQuizzes() {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }
        return quizRepository.findByStatus(Quiz.QuizStatus.DELETED);
    }

    /**
     * ADMIN : Suppression définitive (HARD DELETE)
     * ⚠️ Supprime le quiz, les résultats, les réponses
     */
    @Transactional
    public void permanentlyDeleteQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé - Réservé aux administrateurs");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        // Supprimer toutes les réponses liées à ce quiz
        reponseRepository.deleteByQuizId(quizId);

        // Supprimer tous les résultats liés à ce quiz
        resultatRepository.deleteByQuizId(quizId);

        // Supprimer le quiz
        quizRepository.delete(quiz);
    }

    /**
     * ADMIN : Soft delete (marquer comme supprimé sans effacer)
     */
    @Transactional
    public Quiz softDeleteQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        quiz.setStatus(Quiz.QuizStatus.DELETED);
        quiz.setDeletedAt(LocalDateTime.now());
        quiz.setDeletedBy(admin.getId());

        return quizRepository.save(quiz);
    }

    /**
     * ADMIN : Restaurer un quiz soft-deleté
     */
    @Transactional
    public Quiz restoreQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (quiz.getStatus() != Quiz.QuizStatus.DELETED) {
            throw new RuntimeException("Ce quiz n'est pas supprimé");
        }

        // Vérifier si le quiz n'est pas expiré
        if (quiz.getAvailableUntil() != null && quiz.getAvailableUntil().isBefore(LocalDateTime.now())) {
            quiz.setStatus(Quiz.QuizStatus.EXPIRED);
        } else {
            quiz.setStatus(Quiz.QuizStatus.PUBLISHED);
        }
        quiz.setDeletedAt(null);
        quiz.setDeletedBy(null);

        return quizRepository.save(quiz);
    }

    /**
     * ADMIN : Bloquer un quiz immédiatement (le rendre expiré)
     */
    @Transactional
    public Quiz blockQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        quiz.setStatus(Quiz.QuizStatus.EXPIRED);
        quiz.setAvailableUntil(LocalDateTime.now()); // Expire maintenant

        return quizRepository.save(quiz);
    }

    /**
     * ADMIN : Prolonger la date d'expiration d'un quiz
     */
    @Transactional
    public Quiz extendQuizExpiration(Long quizId, String newExpirationDateStr) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        LocalDateTime newExpirationDate = LocalDateTime.parse(newExpirationDateStr,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        quiz.setAvailableUntil(newExpirationDate);

        // Si le quiz était expiré, le republier
        if (quiz.getStatus() == Quiz.QuizStatus.EXPIRED) {
            quiz.setStatus(Quiz.QuizStatus.PUBLISHED);
        }

        return quizRepository.save(quiz);
    }

    /**
     * ADMIN : Statistiques des quiz
     */
    public Map<String, Long> getQuizStatistics() {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès refusé");
        }

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalQuizzes", quizRepository.count());
        stats.put("publishedQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.PUBLISHED));
        stats.put("expiredQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.EXPIRED));
        stats.put("deletedQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.DELETED));
        stats.put("draftQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.DRAFT));

        return stats;
    }
}