package com.exemple.quiz_app.reponse.service;

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

    @Transactional
    public ReponseDto saveOrUpdateReponse(ReponseRequestDto request) {
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

        // 🔥 VÉRIFICATION DE LA SESSION ET DU TEMPS
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

        boolean alreadyAnswered = reponseRepository.existsByStudentAndQuestionAndQuiz(
                currentUser, question, quiz
        );

        boolean isCorrect = question.checkAnswer(request.getStudentAnswer());
        Integer pointsEarned = isCorrect ? question.getPoints() : 0;

        Reponse reponse;
        if (alreadyAnswered) {
            reponse = reponseRepository.findByStudentAndQuestion(currentUser, question)
                    .orElseThrow(() -> new RuntimeException("Réponse non trouvée"));
            reponse.setStudentAnswer(request.getStudentAnswer());
            reponse.setIsCorrect(isCorrect);
            reponse.setPointsEarned(pointsEarned);
        } else {
            reponse = new Reponse();
            reponse.setQuiz(quiz);
            reponse.setQuestion(question);
            reponse.setStudent(currentUser);
            reponse.setStudentAnswer(request.getStudentAnswer());
            reponse.setIsCorrect(isCorrect);
            reponse.setPointsEarned(pointsEarned);
        }

        Reponse savedReponse = reponseRepository.save(reponse);
        updateResultat(currentUser, quiz);

        return mapToResponseDto(savedReponse);
    }

    /**
     * Soumettre le quiz et retourner score + feedback généré par IA
     */
    @Transactional
    public QuizSubmissionResponseDto submitQuizAndGetResult(List<ReponseRequestDto> requests) {

        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException("Aucune réponse à soumettre");
        }

        Long quizId = requests.get(0).getQuizId();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        User currentUser = authService.getCurrentUser();

        // 🔥 VÉRIFICATION DE LA SESSION ET DU TEMPS AVANT SOUMISSION
        QuizSession session = quizSessionRepository.findByStudentAndQuiz(currentUser, quiz)
                .orElseThrow(() -> new RuntimeException("Session non trouvée. Veuillez démarrer le quiz."));

        if (session.isExpired()) {
            throw new RuntimeException("Temps écoulé ! Vous ne pouvez plus soumettre le quiz.");
        }

        // Marquer la session comme complétée
        session.markAsCompleted();
        quizSessionRepository.save(session);

        // 1. Sauvegarder les réponses
        for (ReponseRequestDto request : requests) {
            try {
                saveOrUpdateReponse(request);
            } catch (Exception e) {
                System.err.println("Erreur sauvegarde: " + e.getMessage());
            }
        }

        // 2. Récupérer toutes les questions et réponses
        List<Question> questions = questionRepository.findByQuizId(quizId);
        List<Reponse> existingReponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);
        Map<Long, Reponse> reponseMap = existingReponses.stream()
                .collect(Collectors.toMap(r -> r.getQuestion().getId(), r -> r));

        // 3. Calculer le score
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

        // 4. Sauvegarder le résultat avec génération IA
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

        // 5. Récupérer le feedback généré par l'IA (dans resultatDto)
        String grade = resultatDto.getGrade() != null ? resultatDto.getGrade() : generateFallbackGrade(percentage);
        String feedbackIA = resultatDto.getFeedbackIa();
        String recommendations = resultatDto.getRecommendations();
        String strengths = resultatDto.getStrengths();
        String weaknesses = resultatDto.getWeaknesses();

        // 6. Si l'IA n'a pas fonctionné, utiliser le feedback local
        if (feedbackIA == null || feedbackIA.isEmpty()) {
            feedbackIA = generateFallbackFeedback(percentage, correctCount, answeredCount, questions.size());
            recommendations = generateFallbackRecommendations(percentage, questions.size() - answeredCount, answeredCount - correctCount);
        }

        // 7. Construire le feedback structuré final
        String finalFeedback = buildStructuredFeedback(quiz, percentage, correctCount, answeredCount, questions.size(), feedbackIA, strengths, weaknesses);

        // 8. Retourner la réponse
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

    /**
     * Construire un feedback structuré à partir des données IA
     */
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
     * Feedback fallback (si l'API IA échoue)
     */
    private String generateFallbackFeedback(double percentage, int correctCount, int answeredCount, int totalQuestions) {
        if (percentage >= 85) {
            return "🎉 EXCELLENT ! Vous maîtrisez parfaitement le sujet. Félicitations pour ce brillant résultat !";
        } else if (percentage >= 70) {
            return "👍 TRÈS BIEN ! Bonne maîtrise du sujet. Quelques points à améliorer pour être parfait.";
        } else if (percentage >= 55) {
            return "📖 BIEN ! Vous avez les bases. Revoyez les erreurs pour progresser.";
        } else if (percentage >= 40) {
            return "⚠️ RÉSULTAT MOYEN. Une révision s'impose pour valider le module.";
        } else {
            return "❌ RÉSULTAT INSUFFISANT. Nous vous conseillons de reprendre le cours.";
        }
    }

    /**
     * Recommandations fallback (si l'API IA échoue)
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
     * Grade fallback (si l'API IA échoue)
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
     * Récupérer les corrections détaillées pour le bouton "Voir corrections"
     */
    public List<ReponseDetailDto> getCorrectionsDetails(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        List<Reponse> reponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);

        if (reponses.isEmpty()) {
            throw new RuntimeException("Vous n'avez pas encore répondu à des questions");
        }

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
                    .pointsEarned(reponse != null && reponse.getPointsEarned() != null ? reponse.getPointsEarned() : 0)
                    .pointsMax(question.getPoints())
                    .options(question.getAllOptions())
                    .explanation(generateExplanation(question, reponse))
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Générer une explication pour chaque question
     */
    private String generateExplanation(Question question, Reponse reponse) {
        if (reponse == null) {
            return "⚠️ Vous n'avez pas répondu à cette question.\n\n" +
                    "💡 La bonne réponse était : " + question.getCorrectAnswerText();
        }

        if (reponse.getIsCorrect()) {
            String[] messages = {"Excellent !", "Bravo !", "Bien joué !", "Parfait !", "Continuez comme ça !"};
            String randomMessage = messages[(int)(Math.random() * messages.length)];
            return "✓ Bonne réponse ! " + randomMessage;
        } else {
            StringBuilder explanation = new StringBuilder();
            explanation.append("✗ Mauvaise réponse.\n\n");
            explanation.append("Votre réponse : ").append(reponse.getStudentAnswer()).append("\n");
            explanation.append("Réponse correcte : ").append(question.getCorrectAnswerText()).append("\n\n");

            if (question.getType() == Question.QuestionType.MCQ) {
                explanation.append("💡 Conseil : Relisez le cours sur ce sujet pour bien comprendre la différence entre les options.");
            } else if (question.getType() == Question.QuestionType.TRUE_FALSE) {
                explanation.append("💡 Conseil : Vérifiez bien les concepts clés avant de répondre Vrai/Faux.");
            } else {
                explanation.append("💡 Conseil : Reformulez la réponse avec vos propres mots pour mieux retenir.");
            }

            return explanation.toString();
        }
    }

    @Transactional
    public List<ReponseDto> saveAllReponses(List<ReponseRequestDto> requests) {
        return requests.stream()
                .map(this::saveOrUpdateReponse)
                .collect(Collectors.toList());
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

    public List<ReponseDto> getReponsesByStudentAndQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));
        List<Reponse> reponses = reponseRepository.findByStudentAndQuiz(currentUser, quiz);
        return reponses.stream()
                .map(this::mapToResponseDto)
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
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
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

    private ReponseDto mapToResponseDto(Reponse reponse) {
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
                .feedback(reponse.getIsCorrect() != null && reponse.getIsCorrect() ?
                        "✓ Bonne réponse !" :
                        "✗ Mauvaise réponse. La bonne réponse était : " +
                                (question != null ? question.getCorrectAnswerText() : "Inconnue"))
                .build();
    }
}