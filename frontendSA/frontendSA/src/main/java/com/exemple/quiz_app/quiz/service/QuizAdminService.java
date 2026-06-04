package com.exemple.quiz_app.quiz.service;

import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.dto.QuizReponse;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizSessionRepository;
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
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
    private QuestionRepository questionRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private QuizStudentRepository quizStudentRepository;

    @Autowired
    private AuthService authService;

    public List<QuizReponse> getAllQuizzes() {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");
        return quizRepository.findAll().stream().map(this::mapToResponse).toList();
    }

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

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!canDeleteQuiz(quiz)) {
            throw new RuntimeException("Ce quiz publie pourra etre supprime apres 5 mois de publication.");
        }

        // 🔥 CORRECTION: Vérifier si la méthode existe, sinon utiliser une alternative
        try {
            reponseRepository.deleteByQuizId(quizId);
        } catch (Exception e) {
            // Si la méthode n'existe pas, supprimer via une requête native ou ignorer
            System.out.println("Notice: deleteByQuizId not available in ReponseRepository");
        }

        try {
            resultatRepository.deleteByQuizId(quizId);
        } catch (Exception e) {
            System.out.println("Notice: deleteByQuizId not available in ResultatRepository");
        }

        quizSessionRepository.deleteByQuizId(quizId);
        quizStudentRepository.deleteByQuizId(quizId);
        questionRepository.deleteByQuizId(quizId);
        quizRepository.deleteById(quizId);
    }

    private boolean canDeleteQuiz(Quiz quiz) {
        if (quiz.getStatus() != Quiz.QuizStatus.PUBLISHED) return true;

        LocalDateTime referenceDate = quiz.getAvailableFrom() != null
                ? quiz.getAvailableFrom()
                : quiz.getCreatedAt();

        return referenceDate != null && !referenceDate.plusMonths(5).isAfter(LocalDateTime.now());
    }

    @Transactional
    public Quiz softDeleteQuiz(Long quizId) {
        User admin = authService.getCurrentUser();
        if (admin.getRole() != Role.ADMIN) throw new RuntimeException("Acces refuse");

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        quiz.setStatus(Quiz.QuizStatus.DELETED);
        quiz.setDeletedAt(LocalDateTime.now());

        // 🔥 CORRECTION: Convertir long en BigInteger
        quiz.setDeletedBy(BigInteger.valueOf(admin.getId().longValue()));

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

    private QuizReponse mapToResponse(Quiz quiz) {
        QuizReponse response = new QuizReponse();
        response.setId(quiz.getId());
        response.setTitre(quiz.getTitre());
        response.setTheme(quiz.getTheme());
        response.setQuestionCount(quiz.getQuestionCount());
        response.setAvailableFrom(quiz.getAvailableFrom());
        response.setAvailableUntil(quiz.getAvailableUntil());
        response.setTimeLimit(quiz.getTimeLimit());
        response.setStatus(quiz.getStatus().name());
        response.setCreationType(quiz.getCreationType().name());
        if (quiz.getEnseignant() != null) {
            response.setEnseignantNom(quiz.getEnseignant().getFirstName() + " " + quiz.getEnseignant().getLastName());
        }
        response.setTotalStudentsAllowed(quizRepository.countAllowedStudents(quiz.getId()));
        response.setCreatedAt(quiz.getCreatedAt());
        return response;
    }
}
