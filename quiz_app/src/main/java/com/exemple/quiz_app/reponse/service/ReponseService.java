package com.exemple.quiz_app.reponse.service;

import com.exemple.quiz_app.AI.client.GeminiApiClient;
import com.exemple.quiz_app.AI.dto.AiCorrectionRequestDto;
import com.exemple.quiz_app.AI.dto.AiCorrectionResponseDto;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.entity.QuizSession;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.quiz.repository.QuizSessionRepository;
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
import com.exemple.quiz_app.reponse.dto.ReponseDetailDto;
import com.exemple.quiz_app.reponse.dto.ReponseDto;
import com.exemple.quiz_app.reponse.dto.ReponseRequestDto;
import com.exemple.quiz_app.reponse.dto.QuizSubmissionResponseDto;
import com.exemple.quiz_app.reponse.entity.Reponse;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import com.exemple.quiz_app.resultat.dto.ResultatDto;
import com.exemple.quiz_app.resultat.dto.ResultatRequestDto;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import com.exemple.quiz_app.resultat.service.ResultatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReponseService {

    private final ReponseRepository reponseRepository;
    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;
    private final QuizStudentRepository quizStudentRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ResultatService resultatService;
    private final AuthService authService;
    private final ResultatRepository resultatRepository;
    private final GeminiApiClient geminiApiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== SAUVEGARDE D'UNE RÉPONSE (SILENCIEUSE - RETOUR VOID) ====================

    @Transactional
    public void saveOrUpdateReponse(ReponseRequestDto request) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() != Role.ETUDIANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seuls les étudiants peuvent répondre aux quiz");
        }

        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (!quiz.isAvailable()) {
            throw new RuntimeException("Ce quiz n'est pas disponible actuellement");
        }

        // Vérifier si autorisé
        boolean isAuthorized = quizStudentRepository.existsByQuizAndStudent(quiz, currentUser);
        if (!isAuthorized && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Vous n'êtes pas autorisé à participer à ce quiz");
        }

        // Vérification de la session et du temps
        QuizSession session = quizSessionRepository.findByStudentAndQuiz(currentUser, quiz)
                .orElseThrow(() -> new RuntimeException("Session non trouvée. Veuillez démarrer le quiz."));

        if (session.isExpired()) {
            throw new RuntimeException("Temps écoulé ! Vous ne pouvez plus répondre aux questions.");
        }

        // Mettre à jour la dernière activité
        session.updateLastActivity();
        quizSessionRepository.save(session);

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question non trouvée"));

        if (!question.getQuiz().getId().equals(quiz.getId())) {
            throw new RuntimeException("Cette question n'appartient pas au quiz spécifié");
        }

        // EMPÊCHER LA MODIFICATION : Une seule réponse par question
        boolean alreadyAnswered = reponseRepository.existsByStudentAndQuestionAndQuiz(
                currentUser, question, quiz
        );

        if (alreadyAnswered) {
            throw new RuntimeException("Vous avez déjà répondu à cette question. La modification n'est pas autorisée.");
        }

        // CORRECTION PAR IA POUR LES QUESTIONS TEXT
        boolean isCorrect = false;
        Integer pointsEarned = 0;

        if (question.getType() == Question.QuestionType.TEXT) {
            try {
                AiCorrectionRequestDto correctionRequest = AiCorrectionRequestDto.builder()
                        .questionText(question.getEnonce())
                        .expectedAnswer(question.getReponseCorrecte())
                        .studentAnswer(request.getStudentAnswer())
                        .pointsMax(question.getPoints())
                        .language("fr")
                        .build();

                AiCorrectionResponseDto correction = callAiForCorrection(correctionRequest);
                isCorrect = correction.getIsCorrect();
                pointsEarned = correction.getPointsEarned();

            } catch (Exception e) {
                // Fallback si l'IA échoue
                isCorrect = question.checkAnswer(request.getStudentAnswer());
                pointsEarned = isCorrect ? question.getPoints() : 0;
            }
        } else {
            // Pour MCQ et TRUE_FALSE, utiliser la logique standard
            isCorrect = question.checkAnswer(request.getStudentAnswer());
            pointsEarned = isCorrect ? question.getPoints() : 0;
        }

        // Créer et sauvegarder la réponse
        pointsEarned = normalizePointsEarned(question, isCorrect, pointsEarned);

        Reponse reponse = new Reponse();
        reponse.setQuiz(quiz);
        reponse.setQuestion(question);
        reponse.setStudent(currentUser);
        reponse.setStudentAnswer(request.getStudentAnswer());
        reponse.setIsCorrect(isCorrect);
        reponse.setPointsEarned(pointsEarned);

        reponseRepository.save(reponse);
        updateResultat(currentUser, quiz);

        // 🔥 AUCUN RETOUR - méthode void (rien n'est retourné au frontend)
    }

    /**
     * Appeler l'IA pour corriger une réponse TEXT
     */
    private AiCorrectionResponseDto callAiForCorrection(AiCorrectionRequestDto request) {
        String prompt = buildCorrectionPrompt(request);
        String response = geminiApiClient.callGemini(prompt);
        return parseCorrectionResponse(response, request.getPointsMax());
    }

    /**
     * Construire le prompt pour la correction IA
     */
    private String buildCorrectionPrompt(AiCorrectionRequestDto request) {
        return String.format("""
            Tu es un professeur expert. Corrige la réponse de l'étudiant et retourne UNIQUEMENT un JSON valide.

            Question : %s
            Réponse attendue : %s
            Réponse de l'étudiant : %s
            Points maximum : %d

            Consignes :
            1. Si la réponse est correcte ou très proche → isCorrect = true, pointsEarned = pointsMax
            2. Si la réponse est partiellement correcte → isCorrect = false, pointsEarned = max(1, pointsMax/2)
            3. Si la réponse est fausse → isCorrect = false, pointsEarned = 0

            Format JSON attendu :
            {
                "isCorrect": true/false,
                "pointsEarned": 0,
                "feedback": "feedback court",
                "explanation": "explication",
                "similarityScore": 0.0
            }
            """,
                request.getQuestionText(),
                request.getExpectedAnswer(),
                request.getStudentAnswer(),
                request.getPointsMax()
        );
    }

    /**
     * Parser la réponse JSON de l'IA
     */
    private AiCorrectionResponseDto parseCorrectionResponse(String response, int pointsMax) {
        try {
            String cleanedResponse = response.replace("```json", "").replace("```", "").trim();
            com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(cleanedResponse);

            return AiCorrectionResponseDto.builder()
                    .isCorrect(json.has("isCorrect") && json.get("isCorrect").asBoolean())
                    .pointsEarned(json.has("pointsEarned") ? json.get("pointsEarned").asInt() : 0)
                    .feedback(json.has("feedback") ? json.get("feedback").asText() : "")
                    .explanation(json.has("explanation") ? json.get("explanation").asText() : "")
                    .similarityScore(json.has("similarityScore") ? json.get("similarityScore").asDouble() : 0.0)
                    .build();
        } catch (Exception e) {
            return AiCorrectionResponseDto.builder()
                    .isCorrect(false)
                    .pointsEarned(0)
                    .feedback("Erreur de correction")
                    .explanation("")
                    .similarityScore(0.0)
                    .build();
        }
    }

    // ==================== SOUMISSION FINALE (SANS BODY) ====================

    @Transactional
    public QuizSubmissionResponseDto submitQuizAndGetResult(Long quizId) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        User currentUser = authService.getCurrentUser();

        // Vérification de la session et du temps avant soumission
        QuizSession session = quizSessionRepository.findByStudentAndQuiz(currentUser, quiz)
                .orElseThrow(() -> new RuntimeException("Session non trouvée. Veuillez démarrer le quiz."));

        if (session.getStatus() == QuizSession.SessionStatus.COMPLETED) {
            throw new RuntimeException("Quiz deja soumis.");
        }

        // Marquer la session comme complétée
        session.markAsCompleted();
        quizSessionRepository.save(session);

        // Récupérer toutes les questions et réponses déjà sauvegardées
        List<Question> questions = questionRepository.findByQuizId(quizId);
        List<Reponse> existingReponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);
        Map<Long, Reponse> reponseMap = existingReponses.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));

        // Calculer le score
        int totalPointsPossible = 0;
        int earnedPoints = 0;
        int answeredCount = 0;
        int correctCount = 0;

        for (Question question : questions) {
            totalPointsPossible += question.getPoints();
            Reponse reponse = reponseMap.get(question.getId());
            if (reponse != null) {
                answeredCount++;
                if (reponse.getIsCorrect()) {
                    earnedPoints += question.getPoints();
                    correctCount++;
                }
            }
        }

        double percentage = totalPointsPossible > 0 ?
                (earnedPoints * 100.0) / totalPointsPossible : 0;

        // Sauvegarder le résultat avec génération IA
        ResultatRequestDto resultatRequest = ResultatRequestDto.builder()
                .quizId(quizId)
                .isCompleted(true)
                .completedDate(LocalDateTime.now())
                .score((double) earnedPoints)
                .earnedPoints(earnedPoints)
                .totalPoints(totalPointsPossible)
                .scorePercentage(percentage)
                .generateFeedback(true)
                .language("fr")
                .build();

        ResultatDto resultatDto = resultatService.saveOrUpdateResultat(resultatRequest);

        // Récupérer le feedback généré par l'IA
        String grade = resultatDto.getGrade() != null ? resultatDto.getGrade() : generateFallbackGrade(percentage);
        String feedbackIA = resultatDto.getFeedbackIa();
        String recommendations = resultatDto.getRecommendations();
        String strengths = resultatDto.getStrengths();
        String weaknesses = resultatDto.getWeaknesses();

        // Si l'IA n'a pas fonctionné, utiliser le feedback local
        if (feedbackIA == null || feedbackIA.isEmpty()) {
            feedbackIA = generateFallbackFeedback(percentage, correctCount, answeredCount, questions.size());
            recommendations = generateFallbackRecommendations(percentage, questions.size() - answeredCount, answeredCount - correctCount);
        }

        // Construire le feedback structuré final
        String finalFeedback = buildStructuredFeedback(quiz, percentage, correctCount, answeredCount, questions.size(), feedbackIA, strengths, weaknesses);

        // Retourner la réponse
        return QuizSubmissionResponseDto.builder()
                .score((double) earnedPoints)
                .earnedPoints(earnedPoints)
                .totalPoints(totalPointsPossible)
                .percentage(percentage)
                .grade(grade)
                .feedback(finalFeedback)
                .recommendations(recommendations)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .isCompleted(true)
                .build();
    }

    // ==================== CORRECTIONS DÉTAILLÉES ====================

    @Transactional
    public QuizSubmissionResponseDto submitQuizWithAnswers(Long quizId, List<ReponseRequestDto> answers) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));
        User currentUser = authService.getCurrentUser();

        QuizSession session = quizSessionRepository.findByStudentAndQuiz(currentUser, quiz)
                .orElseThrow(() -> new RuntimeException("Session non trouvee. Veuillez demarrer le quiz."));

        if (session.getStatus() == QuizSession.SessionStatus.COMPLETED) {
            throw new RuntimeException("Quiz deja soumis.");
        }

        if (answers != null) {
            for (ReponseRequestDto answer : answers) {
                saveFinalAnswer(currentUser, quiz, quizId, answer);
            }
        }

        return submitQuizAndGetResult(quizId);
    }

    public List<ReponseDetailDto> getCorrectionsDetails(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        List<Reponse> reponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);

        List<Question> questions = questionRepository.findByQuizId(quizId);
        Map<Long, Reponse> reponseMap = reponses.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));

        return questions.stream().map(question -> {
            Reponse reponse = reponseMap.get(question.getId());
            return ReponseDetailDto.builder()
                    .questionId(question.getId())
                    .questionText(question.getEnonce())
                    .studentAnswer(reponse != null ? reponse.getStudentAnswer() : "Non répondue")
                    .correctAnswer(question.getCorrectAnswerText())
                    .isCorrect(reponse != null && reponse.getIsCorrect())
                    .pointsEarned(calculateDisplayedPoints(question, reponse))
                    .pointsMax(question.getPoints())
                    .options(question.getAllOptions())
                    .explanation(generateExplanation(question, reponse))
                    .build();
        }).collect(Collectors.toList());
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Construire un feedback structuré
     */
    private void saveFinalAnswer(User currentUser, Quiz quiz, Long quizId, ReponseRequestDto request) {
        if (request == null || request.getQuestionId() == null) {
            return;
        }

        if (request.getQuizId() != null && !request.getQuizId().equals(quizId)) {
            throw new RuntimeException("La reponse n'appartient pas au quiz soumis.");
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question non trouvee"));

        if (!question.getQuiz().getId().equals(quiz.getId())) {
            throw new RuntimeException("Cette question n'appartient pas au quiz soumis.");
        }

        String studentAnswer = request.getStudentAnswer() == null ? "" : request.getStudentAnswer();
        boolean isCorrect;
        Integer pointsEarned;

        if (question.getType() == Question.QuestionType.TEXT) {
            try {
                AiCorrectionRequestDto correctionRequest = AiCorrectionRequestDto.builder()
                        .questionText(question.getEnonce())
                        .expectedAnswer(question.getReponseCorrecte())
                        .studentAnswer(studentAnswer)
                        .pointsMax(question.getPoints())
                        .language("fr")
                        .build();

                AiCorrectionResponseDto correction = callAiForCorrection(correctionRequest);
                isCorrect = correction.getIsCorrect();
                pointsEarned = correction.getPointsEarned();
            } catch (Exception e) {
                isCorrect = question.checkAnswer(studentAnswer);
                pointsEarned = isCorrect ? question.getPoints() : 0;
            }
        } else {
            isCorrect = question.checkAnswer(studentAnswer);
            pointsEarned = isCorrect ? question.getPoints() : 0;
        }

        pointsEarned = normalizePointsEarned(question, isCorrect, pointsEarned);

        Reponse reponse = reponseRepository.findByStudentAndQuestion(currentUser, question)
                .orElseGet(Reponse::new);
        reponse.setQuiz(quiz);
        reponse.setQuestion(question);
        reponse.setStudent(currentUser);
        reponse.setStudentAnswer(studentAnswer);
        reponse.setIsCorrect(isCorrect);
        reponse.setPointsEarned(pointsEarned);

        reponseRepository.save(reponse);
    }



    private int calculateDisplayedPoints(Question question, Reponse reponse) {
        if (question == null || reponse == null) {
            return 0;
        }

        if (Boolean.TRUE.equals(reponse.getIsCorrect())) {
            return question.getPoints() != null ? question.getPoints() : 1;
        }

        return reponse.getPointsEarned() != null ? reponse.getPointsEarned() : 0;
    }

    private int normalizePointsEarned(Question question, boolean isCorrect, Integer pointsEarned) {
        int maxPoints = question != null && question.getPoints() != null ? question.getPoints() : 1;

        if (isCorrect) {
            return maxPoints;
        }

        if (pointsEarned == null) {
            return 0;
        }

        return Math.max(0, Math.min(pointsEarned, maxPoints));
    }


    private String buildStructuredFeedback(Quiz quiz, double percentage, int correctCount, int answeredCount, int totalQuestions, String feedbackIA, String strengths, String weaknesses) {
        StringBuilder fb = new StringBuilder();

        fb.append("╔══════════════════════════════════════════════════════════════╗\n");
        fb.append("║                    📊 RÉSULTAT DU QUIZ                        ║\n");
        fb.append("╚══════════════════════════════════════════════════════════════╝\n\n");

        fb.append("📝 Quiz : ").append(quiz.getTitre()).append("\n");
        fb.append("🎯 Thème : ").append(quiz.getTheme() != null ? quiz.getTheme() : "Général").append("\n\n");

        fb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        fb.append("📊 SCORE : ").append(String.format("%.1f", percentage)).append("%\n");
        fb.append("⭐ MENTION : ").append(generateFallbackGrade(percentage)).append("\n");
        fb.append("✅ Réponses correctes : ").append(correctCount).append("/").append(totalQuestions).append("\n");

        if (answeredCount < totalQuestions) {
            fb.append("⚠️ Questions sans réponse : ").append(totalQuestions - answeredCount).append("\n");
        }

        fb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        if (feedbackIA != null && !feedbackIA.isEmpty()) {
            fb.append("🤖 ANALYSE IA :\n\n");
            fb.append(feedbackIA).append("\n\n");
        }

        if (strengths != null && !strengths.isEmpty()) {
            fb.append("💪 POINTS FORTS :\n");
            fb.append(strengths).append("\n\n");
        }

        if (weaknesses != null && !weaknesses.isEmpty()) {
            fb.append("📌 POINTS À AMÉLIORER :\n");
            fb.append(weaknesses).append("\n\n");
        }

        fb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        return fb.toString();
    }

    /**
     * Feedback fallback
     */
    private String generateFallbackFeedback(double percentage, int correctCount, int answeredCount, int totalQuestions) {
        if (percentage >= 85) {
            return "🎉 EXCELLENT ! Vous maîtrisez parfaitement le sujet.";
        } else if (percentage >= 70) {
            return "👍 TRÈS BIEN ! Bonne maîtrise du sujet.";
        } else if (percentage >= 55) {
            return "📖 BIEN ! Vous avez les bases.";
        } else if (percentage >= 40) {
            return "⚠️ RÉSULTAT MOYEN. Une révision s'impose.";
        } else {
            return "❌ RÉSULTAT INSUFFISANT. Reprenez le cours.";
        }
    }

    /**
     * Recommandations fallback
     */
    private String generateFallbackRecommendations(double percentage, int unansweredCount, int incorrectCount) {
        List<String> recos = new ArrayList<>();
        if (unansweredCount > 0) {
            recos.add("📌 " + unansweredCount + " question(s) sans réponse");
        }
        if (incorrectCount > 0) {
            recos.add("📌 " + incorrectCount + " erreur(s) à corriger");
        }
        if (percentage < 60) {
            recos.add("📌 Reprendre le cours sur ce thème");
        }
        if (recos.isEmpty()) {
            return "📌 Continuez vos efforts !";
        }
        return String.join("\n", recos);
    }

    /**
     * Grade fallback
     */
    private String generateFallbackGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }

    /**
     * Générer une explication simple
     */
    private String generateExplanation(Question question, Reponse reponse) {
        if (reponse == null) {
            return "Non répondue - Bonne réponse : " + question.getCorrectAnswerText();
        }

        if (reponse.getIsCorrect()) {
            return "✓ Correct";
        } else {
            return "✗ Incorrect\nVotre réponse : " + reponse.getStudentAnswer() +
                    "\nBonne réponse : " + question.getCorrectAnswerText();
        }
    }

    private void updateResultat(User student, Quiz quiz) {
        Integer totalPointsEarned = reponseRepository.sumPointsEarnedByStudentAndQuiz(student, quiz);
        if (totalPointsEarned == null) totalPointsEarned = 0;

        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        int totalPointsPossible = questions.stream().mapToInt(Question::getPoints).sum();
        double scorePercentage = totalPointsPossible > 0 ?
                (totalPointsEarned * 100.0) / totalPointsPossible : 0;

        ResultatRequestDto resultatRequest = new ResultatRequestDto();
        resultatRequest.setQuizId(quiz.getId());
        resultatRequest.setScore((double) totalPointsEarned);
        resultatRequest.setTotalPoints(totalPointsPossible);
        resultatRequest.setEarnedPoints(totalPointsEarned);
        resultatRequest.setScorePercentage(scorePercentage);
        resultatRequest.setIsCompleted(false);

        resultatService.saveOrUpdateResultat(resultatRequest);
    }

    // ==================== MÉTHODES POUR COMPATIBILITÉ (peuvent être supprimées si non utilisées) ====================

    @Transactional
    public List<ReponseDto> saveAllReponses(List<ReponseRequestDto> requests) {
        List<ReponseDto> responses = new ArrayList<>();
        for (ReponseRequestDto request : requests) {
            saveOrUpdateReponse(request);
            // Ne pas retourner de DTO car méthode void
        }
        return responses;
    }

    public List<ReponseDto> getReponsesByStudentAndQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        List<Reponse> reponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);
        return reponses.stream()
                .map(this::mapToMinimalDto)
                .collect(Collectors.toList());
    }

    public List<ReponseDto> getReponsesByQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != Role.ENSEIGNANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès non autorisé. Seuls les enseignants peuvent voir toutes les réponses.");
        }
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        List<Reponse> reponses = reponseRepository.findByQuizWithStudent(quiz);
        return reponses.stream()
                .map(this::mapToMinimalDto)
                .collect(Collectors.toList());
    }

    private ReponseDto mapToMinimalDto(Reponse reponse) {
        Question question = reponse.getQuestion();
        return ReponseDto.builder()
                .id(reponse.getId())
                .quizId(reponse.getQuiz() != null ? reponse.getQuiz().getId() : null)
                .quizTitle(reponse.getQuiz() != null ? reponse.getQuiz().getTitre() : null)
                .questionId(question != null ? question.getId() : null)
                .questionText(question != null ? question.getEnonce() : null)
                .questionType(question != null && question.getType() != null ? question.getType().name() : "MCQ")
                .studentAnswer(reponse.getStudentAnswer())
                .correctAnswer(question != null ? question.getCorrectAnswerText() : null)
                .isCorrect(reponse.getIsCorrect())
                .pointsEarned(reponse.getPointsEarned())
                .pointsMax(question != null ? question.getPoints() : 0)
                .answeredAt(reponse.getAnsweredAt())
                .feedback(null)
                .build();
    }

    @Transactional
    public void deleteReponsesByStudentAndQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        if (currentUser.getRole() != Role.ETUDIANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Accès non autorisé");
        }
        reponseRepository.deleteByStudentAndQuiz(currentUser, quiz);
        resultatService.deleteResultatByStudentAndQuiz(quizId);
    }

    public boolean isQuizCompleted(Long quizId) {
        User currentUser = authService.getCurrentUser();
        return resultatRepository.hasStudentCompletedQuiz(currentUser.getId(), quizId);
    }

    public Integer getCurrentScore(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        Integer score = reponseRepository.sumPointsEarnedByStudentAndQuiz(currentUser, quiz);
        return score != null ? score : 0;
    }

    public boolean isStudentAuthorizedForQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        if (currentUser.getRole() == Role.ADMIN) return true;
        if (currentUser.getRole() == Role.ENSEIGNANT) {
            return quiz.getEnseignant().getId().equals(currentUser.getId());
        }
        return quizStudentRepository.existsByQuizAndStudent(quiz, currentUser);
    }
}

