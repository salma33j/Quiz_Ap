package com.exemple.quiz_app.quiz.service;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.quiz.dto.QuizForStudentDto;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudentQuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private AuthService authService;

    public List<QuizForStudentDto> getAvailableQuizzes() {
        User student = authService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        return quizRepository.findAvailableQuizzesForStudent(student, now).stream().map(quiz -> {
            QuizForStudentDto dto = new QuizForStudentDto();
            dto.setId(quiz.getId());
            dto.setTitre(quiz.getTitre());
            dto.setTheme(quiz.getTheme());
            dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
            dto.setQuestionCount(quiz.getQuestionCount());
            dto.setTimeLimit(quiz.getTimeLimit());
            dto.setAvailableUntil(quiz.getAvailableUntil());

            boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quiz.getId());
            if (completed) {
                dto.setStatus("Termine");
                dto.setTimeRemainingSeconds(0L);
            } else {
                dto.setStatus("A faire");
                if (quiz.getAvailableUntil() != null) {
                    long remaining = ChronoUnit.SECONDS.between(now, quiz.getAvailableUntil());
                    dto.setTimeRemainingSeconds(Math.max(0, remaining));
                } else {
                    dto.setTimeRemainingSeconds(-1L);
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public List<QuizForStudentDto> getQuizHistory() {
        User student = authService.getCurrentUser();

        return quizRepository.findAllQuizzesForStudent(student).stream().map(quiz -> {
            QuizForStudentDto dto = new QuizForStudentDto();
            dto.setId(quiz.getId());
            dto.setTitre(quiz.getTitre());
            dto.setTheme(quiz.getTheme());
            dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
            dto.setQuestionCount(quiz.getQuestionCount());
            dto.setTimeLimit(quiz.getTimeLimit());
            dto.setAvailableUntil(quiz.getAvailableUntil());

            boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quiz.getId());
            if (completed) {
                dto.setStatus("Termine");
            } else if (quiz.getAvailableUntil() != null && quiz.getAvailableUntil().isBefore(LocalDateTime.now())) {
                dto.setStatus("Expire");
            } else {
                dto.setStatus("Non commence");
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public QuizForStudentDto getQuizDetails(Long quizId) {
        User student = authService.getCurrentUser();

        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            throw new RuntimeException("Acces non autorise");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        QuizForStudentDto dto = new QuizForStudentDto();
        dto.setId(quiz.getId());
        dto.setTitre(quiz.getTitre());
        dto.setTheme(quiz.getTheme());
        dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
        dto.setQuestionCount(quiz.getQuestionCount());
        dto.setTimeLimit(quiz.getTimeLimit());
        dto.setAvailableUntil(quiz.getAvailableUntil());

        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            dto.setStatus("Termine");
        } else if (!quiz.isAvailable()) {
            dto.setStatus("Expire");
        } else {
            dto.setStatus("A faire");
        }
        return dto;
    }

    public Map<String, Object> canParticipate(Long quizId) {
        User student = authService.getCurrentUser();
        Map<String, Object> result = new HashMap<>();

        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            result.put("canParticipate", false);
            result.put("reason", "Vous n'etes pas autorise");
            return result;
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        if (!quiz.isAvailable()) {
            result.put("canParticipate", false);
            result.put("reason", "Quiz non disponible");
            return result;
        }

        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            result.put("canParticipate", false);
            result.put("reason", "Quiz deja complete");
            return result;
        }

        result.put("canParticipate", true);
        result.put("quizId", quizId);
        result.put("timeLimit", quiz.getTimeLimit());
        result.put("questionCount", quiz.getQuestionCount());
        result.put("availableUntil", quiz.getAvailableUntil());
        return result;
    }

    /** Nom affiché de l'enseignant : pas de colonne "nom" en base, seulement firstName + lastName sur {@link User}. */
    private static String teacherDisplayName(User enseignant) {
        if (enseignant == null) {
            return null;
        }
        return enseignant.getFullName();
    }
}