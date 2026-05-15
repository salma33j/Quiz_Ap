package com.exemple.quiz_app.quiz.service;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.dto.QuestionDto;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.dto.QuizForStudentDto;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.entity.QuizSession;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizSessionRepository;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentQuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

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

    /**
     * Vérifier si l'étudiant peut participer au quiz
     * Vérifie : autorisation, disponibilité (date), non complété, session non expirée
     */
    public Map<String, Object> canParticipate(Long quizId) {
        User student = authService.getCurrentUser();
        Map<String, Object> result = new HashMap<>();

        // Vérifier si autorisé
        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            result.put("canParticipate", false);
            result.put("reason", "Vous n'êtes pas autorisé à participer à ce quiz");
            return result;
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        // Vérifier si le quiz est disponible (date)
        if (!quiz.isAvailable()) {
            result.put("canParticipate", false);
            result.put("reason", "Ce quiz n'est plus disponible (date expirée)");
            return result;
        }

        // Vérifier si déjà complété
        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            result.put("canParticipate", false);
            result.put("reason", "Vous avez déjà complété ce quiz");
            return result;
        }

        // Vérifier la session existante
        Optional<QuizSession> existingSession = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (existingSession.isPresent()) {
            QuizSession session = existingSession.get();

            // Si la session est expirée
            if (session.isExpired()) {
                result.put("canParticipate", false);
                result.put("reason", "Votre temps est écoulé ! Vous ne pouvez plus répondre.");
                result.put("timeExpired", true);
                return result;
            }

            // Si déjà complété
            if (session.getStatus() == QuizSession.SessionStatus.COMPLETED) {
                result.put("canParticipate", false);
                result.put("reason", "Quiz déjà soumis");
                return result;
            }

            // Session active, retourner le temps restant
            result.put("canParticipate", true);
            result.put("quizId", quizId);
            result.put("timeLimit", quiz.getTimeLimit());
            result.put("questionCount", quiz.getQuestionCount());
            result.put("availableUntil", quiz.getAvailableUntil());
            result.put("remainingSeconds", session.getRemainingSeconds());
            result.put("sessionExists", true);
            result.put("sessionId", session.getId());
            return result;
        }

        // Pas de session existante → peut commencer
        result.put("canParticipate", true);
        result.put("quizId", quizId);
        result.put("timeLimit", quiz.getTimeLimit());
        result.put("questionCount", quiz.getQuestionCount());
        result.put("availableUntil", quiz.getAvailableUntil());
        result.put("remainingSeconds", quiz.getTimeLimit() != null ? quiz.getTimeLimit() * 60 : -1);
        result.put("sessionExists", false);

        return result;
    }

    /**
     * Démarrer un quiz (créer une session)
     * Appelé quand l'étudiant clique sur "Commencer le quiz"
     */
    @Transactional
    public void startQuiz(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        // Vérifier si déjà complété
        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            throw new RuntimeException("Vous avez déjà complété ce quiz");
        }

        // Vérifier si le quiz est disponible
        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est plus disponible");
        }

        // Vérifier si une session existe déjà
        Optional<QuizSession> existingSession = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (existingSession.isPresent()) {
            QuizSession session = existingSession.get();
            if (session.isExpired()) {
                throw new RuntimeException("Votre session a expiré. Vous ne pouvez plus répondre.");
            }
            if (session.getStatus() == QuizSession.SessionStatus.COMPLETED) {
                throw new RuntimeException("Quiz déjà soumis");
            }
            // Session active, continuer
            return;
        }

        // Créer nouvelle session
        QuizSession session = new QuizSession();
        session.setQuiz(quiz);
        session.setStudent(student);
        session.setStatus(QuizSession.SessionStatus.ACTIVE);
        quizSessionRepository.save(session);
    }

    /**
     * Vérifier le temps restant (appel périodique par le frontend)
     */
    public Map<String, Object> getRemainingTime(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        Map<String, Object> result = new HashMap<>();

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (session.isEmpty()) {
            result.put("hasSession", false);
            result.put("canContinue", false);
            result.put("reason", "Aucune session trouvée");
            return result;
        }

        QuizSession quizSession = session.get();

        if (quizSession.isExpired()) {
            result.put("hasSession", true);
            result.put("canContinue", false);
            result.put("isExpired", true);
            result.put("reason", "Temps écoulé !");
            result.put("remainingSeconds", 0L);
            return result;
        }

        result.put("hasSession", true);
        result.put("canContinue", true);
        result.put("isExpired", false);
        result.put("remainingSeconds", quizSession.getRemainingSeconds());
        result.put("timeLimitMinutes", quiz.getTimeLimit());

        return result;
    }

    /**
     * Marquer la session comme complétée (après soumission)
     */
    @Transactional
    public void completeSession(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
        if (session.isPresent()) {
            session.get().markAsCompleted();
            quizSessionRepository.save(session.get());
        }
    }

    /**
     * Récupérer le temps restant en secondes pour le frontend (timer)
     */
    public Long getRemainingSecondsForQuiz(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (session.isPresent() && !session.get().isExpired()) {
            return session.get().getRemainingSeconds();
        }

        // Si pas de session, retourner la limite totale
        if (quiz.getTimeLimit() != null) {
            return quiz.getTimeLimit() * 60L;
        }

        return -1L;
    }

    /**
     * 🔥 Récupérer les questions d'un quiz pour l'étudiant (sans réponses correctes)
     */
    public List<QuestionDto> getQuizQuestions(Long quizId) {
        User student = authService.getCurrentUser();

        // Vérifier si autorisé
        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            throw new RuntimeException("Accès non autorisé");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        // Vérifier si le quiz est disponible
        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est plus disponible");
        }

        // Vérifier si déjà complété
        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            throw new RuntimeException("Vous avez déjà complété ce quiz");
        }

        // Vérifier si la session n'est pas expirée
        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
        if (session.isPresent() && session.get().isExpired()) {
            throw new RuntimeException("Temps écoulé ! Vous ne pouvez plus répondre");
        }

        // Récupérer les questions sans les réponses correctes
        return questionRepository.findByQuizId(quizId).stream()
                .map(this::mapToStudentQuestionDto)
                .collect(Collectors.toList());
    }

    /**
     * Mapper Question → QuestionDto (sans réponse correcte)
     */
    private QuestionDto mapToStudentQuestionDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setEnonce(question.getEnonce());
        dto.setOptions(question.getAllOptions());
        dto.setType(question.getType().name());
        dto.setPoints(question.getPoints());
        return dto;
    }

    /**
     * Nom affiché de l'enseignant
     */
    private static String teacherDisplayName(User enseignant) {
        if (enseignant == null) {
            return null;
        }
        return enseignant.getFullName();
    }
}