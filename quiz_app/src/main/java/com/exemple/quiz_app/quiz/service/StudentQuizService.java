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
     * Quizzes disponibles (uniquement "Ã€ faire")
     * Ne montre PAS les quiz terminÃ©s, expirÃ©s, ou avec session expirÃ©e
     */
    @Transactional
    public List<QuizForStudentDto> getAvailableQuizzes() {
        User student = authService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        Long classId = getStudentClassId(student);
        expirePublishedQuizzesPastDeadline(now);

        return quizRepository.findAvailableQuizzesForStudent(student.getId(), classId, now).stream()
                .filter(quiz -> {
                    // ðŸ”¥ Exclure les quiz dÃ©jÃ  complÃ©tÃ©s (soumis)
                    boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quiz.getId());
                    if (completed) return false;

                    // ðŸ”¥ Exclure les quiz avec session expirÃ©e
                    Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
                    return session.isEmpty()
                            || (session.get().getStatus() == QuizSession.SessionStatus.ACTIVE
                            && !session.get().isExpired());
                })
                .map(quiz -> {
                    QuizForStudentDto dto = new QuizForStudentDto();
                    dto.setId(quiz.getId());
                    dto.setTitre(quiz.getTitre());
                    dto.setTheme(quiz.getTheme());
                    dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
                    dto.setQuestionCount(quiz.getQuestionCount());
                    dto.setTimeLimit(quiz.getTimeLimit());
                    dto.setAvailableFrom(quiz.getAvailableFrom());
                    dto.setAvailableUntil(quiz.getAvailableUntil());
                    applyQuizContext(dto, quiz);
                    dto.setStatus("Ã€ faire");

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
     * - TerminÃ© = soumis OU session expirÃ©e
     * - ExpirÃ© = date du professeur dÃ©passÃ©e uniquement
     * - Ã€ faire = quiz disponible
     */
    @Transactional
    public List<QuizForStudentDto> getQuizHistory() {
        User student = authService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        Long classId = getStudentClassId(student);
        expirePublishedQuizzesPastDeadline(now);

        return quizRepository.findAllQuizzesForStudent(student.getId(), classId).stream().map(quiz -> {
            QuizForStudentDto dto = new QuizForStudentDto();
            dto.setId(quiz.getId());
            dto.setTitre(quiz.getTitre());
            dto.setTheme(quiz.getTheme());
            dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
            dto.setQuestionCount(quiz.getQuestionCount());
            dto.setTimeLimit(quiz.getTimeLimit());
            dto.setAvailableFrom(quiz.getAvailableFrom());
            dto.setAvailableUntil(quiz.getAvailableUntil());
            applyQuizContext(dto, quiz);

            boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quiz.getId());
            Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
            boolean isSessionExpired = session.isPresent() && session.get().isExpired();

            // ðŸ”¥ 1. VÃ©rifier si la date du professeur est dÃ©passÃ©e -> EXPIRÃ‰
            boolean isDateExpired = quiz.getAvailableUntil() != null && now.isAfter(quiz.getAvailableUntil());

            if (isDateExpired) {
                dto.setStatus("ExpirÃ©");
                dto.setTimeRemainingSeconds(0L);
            }
            // ðŸ”¥ 2. VÃ©rifier si terminÃ© (soumis OU session expirÃ©e) -> TERMINÃ‰
            else if (completed || isSessionExpired) {
                dto.setStatus("TerminÃ©");
                dto.setTimeRemainingSeconds(0L);
            }
            // ðŸ”¥ 3. Sinon -> Ã€ FAIRE
            else {
                dto.setStatus("Ã€ faire");
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

    @Transactional
    public QuizForStudentDto getQuizDetails(Long quizId) {
        User student = authService.getCurrentUser();
        expirePublishedQuizzesPastDeadline(LocalDateTime.now());

        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            throw new RuntimeException("AccÃ¨s non autorisÃ©");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvÃ©"));

        QuizForStudentDto dto = new QuizForStudentDto();
        dto.setId(quiz.getId());
        dto.setTitre(quiz.getTitre());
        dto.setTheme(quiz.getTheme());
        dto.setEnseignantNom(teacherDisplayName(quiz.getEnseignant()));
        dto.setQuestionCount(quiz.getQuestionCount());
        dto.setTimeLimit(quiz.getTimeLimit());
        dto.setAvailableFrom(quiz.getAvailableFrom());
        dto.setAvailableUntil(quiz.getAvailableUntil());
        applyQuizContext(dto, quiz);

        boolean completed = resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId);
        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
        boolean isSessionExpired = session.isPresent() && session.get().isExpired();
        boolean isDateExpired = quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil());

        if (isDateExpired) {
            dto.setStatus("ExpirÃ©");
        } else if (completed || isSessionExpired) {
            dto.setStatus("TerminÃ©");
        } else if (!quiz.isAvailable()) {
            dto.setStatus("ExpirÃ©");
        } else {
            dto.setStatus("Ã€ faire");
        }
        return dto;
    }

    /**
     * VÃ©rifier si l'Ã©tudiant peut participer au quiz
     */
    public Map<String, Object> canParticipate(Long quizId) {
        User student = authService.getCurrentUser();
        Map<String, Object> result = new HashMap<>();

        // VÃ©rifier si autorisÃ©
        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            result.put("canParticipate", false);
            result.put("reason", "Vous n'Ãªtes pas autorisÃ© Ã  participer Ã  ce quiz");
            return result;
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvÃ©"));

        // VÃ©rifier si la date du professeur est dÃ©passÃ©e
        if (quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil())) {
            result.put("canParticipate", false);
            result.put("reason", "Ce quiz a expirÃ© (date dÃ©passÃ©e)");
            return result;
        }

        // VÃ©rifier si le quiz est disponible
        if (!quiz.isAvailable()) {
            result.put("canParticipate", false);
            result.put("reason", "Ce quiz n'est pas disponible");
            return result;
        }

        // VÃ©rifier si dÃ©jÃ  complÃ©tÃ©
        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            result.put("canParticipate", false);
            result.put("reason", "Vous avez dÃ©jÃ  complÃ©tÃ© ce quiz");
            return result;
        }

        // VÃ©rifier la session existante
        Optional<QuizSession> existingSession = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (existingSession.isPresent()) {
            QuizSession quizSession = existingSession.get();
            boolean canContinue = quizSession.getStatus() == QuizSession.SessionStatus.ACTIVE
                    && !quizSession.isExpired();

            result.put("canParticipate", canContinue);
            result.put("quizId", quizId);
            result.put("sessionExists", true);
            result.put("remainingSeconds", quizSession.getRemainingSeconds());
            result.put("reason", canContinue
                    ? "Session deja ouverte, reprise du quiz."
                    : "Vous avez deja ouvert ce quiz et la session est expiree.");
            return result;
        }

        
        // Pas de session existante â†’ peut commencer
        result.put("canParticipate", true);
        result.put("quizId", quizId);
        result.put("timeLimit", quiz.getTimeLimit());
        result.put("questionCount", quiz.getQuestionCount());
        result.put("availableFrom", quiz.getAvailableFrom());
        result.put("availableUntil", quiz.getAvailableUntil());
        result.put("remainingSeconds", quiz.getTimeLimit() != null ? quiz.getTimeLimit() * 60L : -1L);
        result.put("sessionExists", false);

        return result;
    }

    /**
     * DÃ©marrer un quiz (crÃ©er une session)
     */
    @Transactional
    public void startQuiz(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvÃ©"));

        // VÃ©rifier si dÃ©jÃ  complÃ©tÃ©
        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            throw new RuntimeException("Vous avez dÃ©jÃ  complÃ©tÃ© ce quiz");
        }

        // VÃ©rifier si la date du professeur est dÃ©passÃ©e
        if (quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil())) {
            throw new RuntimeException("Ce quiz a expirÃ©");
        }

        // VÃ©rifier si le quiz est disponible
        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est plus disponible");
        }

        // VÃ©rifier si une session existe dÃ©jÃ 
        Optional<QuizSession> existingSession = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (existingSession.isPresent()) {
            QuizSession quizSession = existingSession.get();
            if (quizSession.getStatus() == QuizSession.SessionStatus.ACTIVE && !quizSession.isExpired()) {
                quizSession.updateLastActivity();
                quizSessionRepository.save(quizSession);
                return;
            }

            throw new RuntimeException("Vous avez deja ouvert ce quiz et la session est expiree.");
        }

        // CrÃ©er nouvelle session
        QuizSession session = new QuizSession();
        session.setQuiz(quiz);
        session.setStudent(student);
        session.setStatus(QuizSession.SessionStatus.ACTIVE);
        quizSessionRepository.save(session);
    }

    /**
     * VÃ©rifier le temps restant
     */
    public Map<String, Object> getRemainingTime(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvÃ©"));

        Map<String, Object> result = new HashMap<>();

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);

        if (session.isEmpty()) {
            result.put("hasSession", false);
            result.put("canContinue", false);
            result.put("reason", "Aucune session trouvÃ©e");
            return result;
        }

        QuizSession quizSession = session.get();

        if (quizSession.isExpired()) {
            result.put("hasSession", true);
            result.put("canContinue", false);
            result.put("isExpired", true);
            result.put("reason", "Temps Ã©coulÃ© !");
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
     * Marquer la session comme complÃ©tÃ©e (aprÃ¨s soumission)
     */
    @Transactional
    public void completeSession(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvÃ©"));

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
        if (session.isPresent()) {
            session.get().markAsCompleted();
            quizSessionRepository.save(session.get());
        }
    }

    /**
     * RÃ©cupÃ©rer le temps restant en secondes
     */
    public Long getRemainingSecondsForQuiz(Long quizId) {
        User student = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvÃ©"));

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
     * RÃ©cupÃ©rer les questions d'un quiz pour l'Ã©tudiant
     */
    public List<QuestionDto> getQuizQuestions(Long quizId) {
        User student = authService.getCurrentUser();

        if (!quizRepository.isStudentAllowed(quizId, student.getId().longValue())) {
            throw new RuntimeException("AccÃ¨s non autorisÃ©");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvÃ©"));

        // VÃ©rifier si la date du professeur est dÃ©passÃ©e
        if (quiz.getAvailableUntil() != null && LocalDateTime.now().isAfter(quiz.getAvailableUntil())) {
            throw new RuntimeException("Ce quiz a expirÃ©");
        }

        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est plus disponible");
        }

        if (resultatRepository.hasStudentCompletedQuiz(student.getId().longValue(), quizId)) {
            throw new RuntimeException("Vous avez dÃ©jÃ  complÃ©tÃ© ce quiz");
        }

        Optional<QuizSession> session = quizSessionRepository.findByStudentAndQuiz(student, quiz);
        if (session.isPresent() && session.get().isExpired()) {
            throw new RuntimeException("Temps Ã©coulÃ© ! Vous ne pouvez plus rÃ©pondre");
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

    private Long getStudentClassId(User student) {
        return student.getClasse() != null ? student.getClasse().getId() : null;
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

    private void expirePublishedQuizzesPastDeadline(LocalDateTime now) {
        quizRepository.expirePublishedQuizzesPastDeadline(now);
    }
}
