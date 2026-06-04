package com.exemple.quiz_app.quiz.service;

import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.classe.entity.Classe;
import com.exemple.quiz_app.matiere.entity.Matiere;
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

    /**
     * Quizzes disponibles (uniquement "À faire")
     * Ne montre PAS les quiz terminés, expirés, ou avec session expirée
     */
    @Transactional(readOnly = true)
    public List<QuizForStudentDto> getAvailableQuizzes() {
        User student = authService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        return quizRepository.findAvailableQuizzesForStudent(student, now).stream()
                .filter(quiz -> {
                    // 🔥 Exclure les quiz déjà complétés (soumis)
                    boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quiz.getId());
                    if (completed) return false;

                    // 🔥 Exclure les quiz avec session expirée
                    Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
                    if (session.isPresent() && session.get().isExpired()) {
                        return false;
                    }

                    return true;
                })
                .map(quiz -> {
                    QuizForStudentDto dto = new QuizForStudentDto();
                    dto.setId(quiz.getId());
                    dto.setTitre(quiz.getTitre());
                    dto.setTheme(quiz.getTheme());
                    dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
                    dto.setQuestionCount(quiz.getQuestionCount());
                    dto.setTimeLimit(quiz.getTimeLimit());
                    dto.setAvailableUntil(quiz.getAvailableUntil());
                    applyQuizContext(dto, quiz);
                    dto.setStatus("À faire");

                    if (quiz.getAvailableUntil() != null) {
                        long remaining = ChronoUnit.SECONDS.between(now, quiz.getAvailableUntil());
                        dto.setTimeRemainingSeconds(Math.max(0, remaining));
                    } else {
                        dto.setTimeRemainingSeconds(-1L);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Historique complet des quizzes
     * - Terminé = soumis OU session expirée
     * - Expiré = date du professeur dépassée uniquement
     * - À faire = quiz disponible
     */
    @Transactional(readOnly = true)
    public List<QuizForStudentDto> getQuizHistory() {
        User student = authService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        return quizRepository.findAllQuizzesForStudent(student).stream().map(quiz -> {
            QuizForStudentDto dto = new QuizForStudentDto();
            dto.setId(quiz.getId());
            dto.setTitre(quiz.getTitre());
            dto.setTheme(quiz.getTheme());
            dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
            dto.setQuestionCount(quiz.getQuestionCount());
            dto.setTimeLimit(quiz.getTimeLimit());
            dto.setAvailableUntil(quiz.getAvailableUntil());
            applyQuizContext(dto, quiz);

            boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quiz.getId());
            Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
            boolean isSessionExpired = session.isPresent() && session.get().isExpired();

            // 🔥 1. Vérifier si la date du professeur est dépassée -> EXPIRÉ
            boolean isDateExpired = quiz.getAvailableUntil() != null && now.isAfter(quiz.getAvailableUntil());

            if (isDateExpired) {
                dto.setStatus("Expiré");
                dto.setTimeRemainingSeconds(0L);
            }
            // 🔥 2. Vérifier si terminé (soumis OU session expirée) -> TERMINÉ
            else if (completed || isSessionExpired) {
                dto.setStatus("Terminé");
                dto.setTimeRemainingSeconds(0L);
            }
            // 🔥 3. Sinon -> À FAIRE
            else {
                dto.setStatus("À faire");
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

    @Transactional(readOnly = true)
    public QuizForStudentDto getQuizDetails(Long quizId) {
        User student = authService.getCurrentUser();

        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            throw new RuntimeException("Accès non autorisé");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        QuizForStudentDto dto = new QuizForStudentDto();
        dto.setId(quiz.getId());
        dto.setTitre(quiz.getTitre());
        dto.setTheme(quiz.getTheme());
        dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
        dto.setQuestionCount(quiz.getQuestionCount());
        dto.setTimeLimit(quiz.getTimeLimit());
        dto.setAvailableUntil(quiz.getAvailableUntil());
        applyQuizContext(dto, quiz);

        boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId);
        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
        boolean isSessionExpired = session.isPresent() && session.get().isExpired();
        boolean isDateExpired = quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil());

        if (isDateExpired) {
            dto.setStatus("Expiré");
        } else if (completed || isSessionExpired) {
            dto.setStatus("Terminé");
        } else if (!quiz.isAvailable()) {
            dto.setStatus("Expiré");
        } else {
            dto.setStatus("À faire");
        }
        return dto;
    }

    /**
     * Vérifier si l'étudiant peut participer au quiz
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

        // Vérifier si la date du professeur est dépassée
        if (quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil())) {
            result.put("canParticipate", false);
            result.put("reason", "Ce quiz a expiré (date dépassée)");
            return result;
        }

        // Vérifier si le quiz est disponible
        if (!quiz.isAvailable()) {
            result.put("canParticipate", false);
            result.put("reason", "Ce quiz n'est pas disponible");
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
                result.put("reason", "Votre temps est écoulé !");
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
        result.put("remainingSeconds", quiz.getTimeLimit() != null ? quiz.getTimeLimit() * 60L : -1L);
        result.put("sessionExists", false);

        return result;
    }

    /**
     * Démarrer un quiz (créer une session)
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

        // Vérifier si la date du professeur est dépassée
        if (quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil())) {
            throw new RuntimeException("Ce quiz a expiré");
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
     * Vérifier le temps restant
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
     * Récupérer le temps restant en secondes
     */
    public Long getRemainingSecondsForQuiz(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (session.isPresent()) {
            QuizSession quizSession = session.get();
            if (quizSession.getStatus() == QuizSession.SessionStatus.COMPLETED || quizSession.isExpired()) {
                return 0L;
            }
            return quizSession.getRemainingSeconds();
        }

        if (quiz.getTimeLimit() != null) {
            return quiz.getTimeLimit() * 60L;
        }

        return -1L;
    }

    /**
     * Récupérer les questions d'un quiz pour l'étudiant
     */
    public List<QuestionDto> getQuizQuestions(Long quizId) {
        User student = authService.getCurrentUser();

        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            throw new RuntimeException("Accès non autorisé");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        // Vérifier si la date du professeur est dépassée
        if (quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil())) {
            throw new RuntimeException("Ce quiz a expiré");
        }

        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est plus disponible");
        }

        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            throw new RuntimeException("Vous avez déjà complété ce quiz");
        }

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
        if (session.isPresent() && session.get().isExpired()) {
            throw new RuntimeException("Temps écoulé ! Vous ne pouvez plus répondre");
        }

        return questionRepository.findByQuizId(quizId).stream()
                .map(this::mapToStudentQuestionDto)
                .collect(Collectors.toList());
    }

    private QuestionDto mapToStudentQuestionDto(Question question) {
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        dto.setEnonce(question.getEnonce());
        dto.setOptions(question.getAllOptions());
        dto.setType(question.getType().name());
        dto.setPoints(question.getPoints());
        return dto;
    }

    private void applyQuizContext(QuizForStudentDto dto, Quiz quiz) {
        Matiere matiere = quiz.getMatiere();
        Classe classe = quiz.getClasse();

        if (matiere != null) {
            dto.setMatiereId(matiere.getId());
            dto.setMatiereName(matiere.getNom());
            dto.setMatiereNom(matiere.getNom());

            if (classe == null) {
                classe = matiere.getClasse();
            }
        }

        if (classe != null) {
            dto.setClassId(classe.getId());
            dto.setClasseId(classe.getId());
            dto.setClassName(classe.getName());
            dto.setClasseName(classe.getName());
            dto.setClassFiliere(classe.getFiliere());
            dto.setClassNiveau(classe.getNiveau());
        }
    }

    private static String teacherDisplayName(User enseignant) {
        if (enseignant == null) {
            return null;
        }
        return enseignant.getFullName();
    }
}
