package com.exemple.quiz_app.quiz.service;

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

    public List<Quiz> getAllExpiredQuizzes() {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");
        return quizRepository.findByStatus(Quiz.QuizStatus.EXPIRED);
    }

    public List<Quiz> getAllDeletedQuizzes() {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");
        return quizRepository.findByStatus(Quiz.QuizStatus.DELETED);
    }

    @Transactional
    public void permanentlyDeleteQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");

        quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        reponseRepository.deleteByQuizId(quizId);
        resultatRepository.deleteByQuizId(quizId);
        quizRepository.deleteById(quizId);
    }

    @Transactional
    public Quiz softDeleteQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        quiz.setStatus(Quiz.QuizStatus.DELETED);
        quiz.setDeletedAt(LocalDateTime.now());
        quiz.setDeletedBy(admin.getId().longValue());
        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz restoreQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (quiz.getStatus() != Quiz.QuizStatus.DELETED) {
            throw new RuntimeException("Ce quiz n'est pas supprime");
        }

        if (quiz.getAvailableUntil() != null && quiz.getAvailableUntil().isBefore(LocalDateTime.now())) {
            quiz.setStatus(Quiz.QuizStatus.EXPIRED);
        } else {
            quiz.setStatus(Quiz.QuizStatus.PUBLISHED);
        }
        quiz.setDeletedAt(null);
        quiz.setDeletedBy(null);
        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz blockQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        quiz.setStatus(Quiz.QuizStatus.EXPIRED);
        quiz.setAvailableUntil(LocalDateTime.now());
        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz extendQuizExpiration(Long quizId, String newExpirationDateStr) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        LocalDateTime newDate = LocalDateTime.parse(newExpirationDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        quiz.setAvailableUntil(newDate);

        if (quiz.getStatus() == Quiz.QuizStatus.EXPIRED) {
            quiz.setStatus(Quiz.QuizStatus.PUBLISHED);
        }
        return quizRepository.save(quiz);
    }

    public Map<String, Long> getQuizStatistics() {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalQuizzes", quizRepository.count());
        stats.put("publishedQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.PUBLISHED));
        stats.put("expiredQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.EXPIRED));
        stats.put("deletedQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.DELETED));
        stats.put("draftQuizzes", quizRepository.countByStatus(Quiz.QuizStatus.DRAFT));
        return stats;
    }
}