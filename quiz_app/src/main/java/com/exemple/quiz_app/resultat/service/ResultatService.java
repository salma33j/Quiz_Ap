package com.exemple.quiz_app.resultat.service;

import com.exemple.quiz_app.AI.dto.FeedbackRequestDto;
import com.exemple.quiz_app.AI.dto.FeedbackResponseDto;
import com.exemple.quiz_app.AI.service.AiFeedbackService;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.reponse.entity.Reponse;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import com.exemple.quiz_app.resultat.dto.ResultatDto;
import com.exemple.quiz_app.resultat.dto.ResultatRequestDto;
import com.exemple.quiz_app.resultat.entity.Resultat;
import com.exemple.quiz_app.resultat.mapper.ResultatMapper;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultatService {

    private final ResultatRepository resultatRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final ReponseRepository reponseRepository;
    private final ResultatMapper resultatMapper;
    private final AuthService authService;
    private final AiFeedbackService aiFeedbackService;

    /**
     * Sauvegarder ou mettre à jour un résultat
     */
    @Transactional
    public ResultatDto saveOrUpdateResultat(ResultatRequestDto resultatRequestDto) {

        User currentUser = authService.getCurrentUser();
        User student = currentUser;
        boolean shouldComplete = Boolean.TRUE.equals(resultatRequestDto.getIsCompleted());

        if (student.getRole() != Role.ETUDIANT && student.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seuls les étudiants peuvent avoir des résultats");
        }

        Quiz quiz = quizRepository.findById(resultatRequestDto.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (quiz.isDeleted()) {
            throw new RuntimeException("Ce quiz n'est plus disponible");
        }

        Resultat resultat = resultatRepository
                .findByStudentAndQuizId(student, quiz.getId())
                .orElse(null);

        if (resultat == null) {
            resultat = resultatMapper.toEntity(resultatRequestDto, quiz, student);
        } else {
            resultatMapper.updateEntity(resultatRequestDto, resultat);
        }

        // Mettre à jour les champs importants même si non complété
        if (resultatRequestDto.getTotalPoints() != null) {
            resultat.setTotalPoints(resultatRequestDto.getTotalPoints());
        }
        if (resultatRequestDto.getEarnedPoints() != null) {
            resultat.setEarnedPoints(resultatRequestDto.getEarnedPoints());
        }
        if (resultatRequestDto.getScorePercentage() != null) {
            resultat.setScorePercentage(resultatRequestDto.getScorePercentage());
        }
        if (resultatRequestDto.getScore() != null) {
            resultat.setScore(resultatRequestDto.getScore());
        }

        // Si le quiz est terminé
        if (shouldComplete) {

            // Recalculer le score final si nécessaire
            if (resultat.getTotalPoints() == null || resultat.getEarnedPoints() == null) {
                calculateFinalScore(resultat, student, quiz);
            }

            // Marquer comme complété (même si déjà fait)
            if (resultat.getStatus() != Resultat.SubmissionStatus.SUBMITTED) {
                resultat.markAsCompleted();
            } else {
                // Si déjà complété, juste mettre à jour la date si nécessaire
                if (resultat.getCompletedDate() == null) {
                    resultat.setCompletedDate(LocalDateTime.now());
                }
            }
            resultat.setGrade(resultat.getGradeLetter());

            // Génération AI si demandé (même si déjà fait, on peut regénérer)
            if (Boolean.TRUE.equals(resultatRequestDto.getGenerateFeedback())) {
                Resultat persistedResultat = resultatRepository.saveAndFlush(resultat);
                return generateAndSaveFeedback(persistedResultat, resultatRequestDto.getLanguage() != null ?
                        resultatRequestDto.getLanguage() : "fr");
            }
        }

        Resultat savedResultat = resultatRepository.save(resultat);
        return resultatMapper.toDto(savedResultat);
    }

    /**
     * Calcul score final
     */
    private void calculateFinalScore(Resultat resultat, User student, Quiz quiz) {

        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        List<Reponse> reponses = reponseRepository.findByStudentAndQuestionQuizId(student, quiz.getId());

        int totalPointsPossible = questions.stream()
                .mapToInt(Question::getPoints)
                .sum();

        int earnedPoints = reponses.stream()
                .mapToInt(r -> r.getPointsEarned() != null ? r.getPointsEarned() : 0)
                .sum();

        double scorePercentage = totalPointsPossible > 0
                ? (earnedPoints * 100.0) / totalPointsPossible
                : 0;

        resultat.setTotalPoints(totalPointsPossible);
        resultat.setEarnedPoints(earnedPoints);
        resultat.setScore((double) earnedPoints);
        resultat.setScorePercentage(scorePercentage);
        resultat.setIsCompleted(true);
        resultat.setGrade(resultat.getGradeLetter());
    }

    /**
     * 🔥 FEEDBACK AI OPTIMISÉ
     */
    @Transactional
    public ResultatDto generateAndSaveFeedback(Resultat resultat, String language) {

        if (resultat == null) {
            throw new RuntimeException("Résultat non trouvé");
        }

        // Si le résultat n'est pas marqué comme complété, on le force
        if (resultat.getStatus() != Resultat.SubmissionStatus.SUBMITTED) {
            resultat.markAsCompleted();
        }

        Quiz quiz = resultat.getQuiz();

        FeedbackRequestDto feedbackRequest = new FeedbackRequestDto();
        feedbackRequest.setResultatId(resultat.getId());
        feedbackRequest.setQuizTitle(quiz.getTitre());
        feedbackRequest.setQuizTheme(quiz.getTheme());
        feedbackRequest.setScore(resultat.getScorePercentage());
        feedbackRequest.setTotalPoints(resultat.getTotalPoints());
        feedbackRequest.setEarnedPoints(resultat.getEarnedPoints());
        feedbackRequest.setLanguage(language != null ? language : "fr");

        // Déterminer niveau étudiant localement
        double score = resultat.getScorePercentage() != null ? resultat.getScorePercentage() : 0;
        String performanceLevel;

        if (score >= 80) {
            performanceLevel = "excellent";
        } else if (score >= 60) {
            performanceLevel = "good";
        } else if (score >= 40) {
            performanceLevel = "average";
        } else {
            performanceLevel = "weak";
        }

        feedbackRequest.setPerformanceLevel(performanceLevel);

        try {
            FeedbackResponseDto feedbackResponse = aiFeedbackService.generateFeedback(feedbackRequest);

            resultat.setFeedbackIa(feedbackResponse.getFeedback());
            resultat.setStrengths(feedbackResponse.getStrengths());
            resultat.setWeaknesses(feedbackResponse.getWeaknesses());
            resultat.setRecommendations(feedbackResponse.getRecommendations());
            resultat.setSuggestedQuiz(feedbackResponse.getSuggestedQuiz());
            resultat.setGrade(feedbackResponse.getGrade());
        } catch (Exception e) {
            // Fallback si l'API AI échoue
            resultat.setFeedbackIa(generateFallbackFeedback(score));
            resultat.setStrengths(getFallbackStrengths(score));
            resultat.setWeaknesses(getFallbackWeaknesses(score));
            resultat.setRecommendations(getFallbackRecommendations(score));
            resultat.setGrade(getFallbackGrade(score));
        }

        resultatRepository.save(resultat);
        return resultatMapper.toDto(resultat);
    }

    /**
     * Feedback fallback si l'API AI est indisponible
     */
    private String generateFallbackFeedback(double score) {
        if (score >= 80) {
            return "🎉 Félicitations ! Excellent résultat de " + String.format("%.1f", score) + "%. Vous maîtrisez parfaitement le sujet.";
        } else if (score >= 60) {
            return "👍 Très bien ! Résultat de " + String.format("%.1f", score) + "%. Bonne maîtrise du sujet, quelques points à améliorer.";
        } else if (score >= 40) {
            return "📚 Bien ! Résultat de " + String.format("%.1f", score) + "%. Vous avez les bases, mais une révision s'impose.";
        } else if (score >= 20) {
            return "⚠️ Résultat insuffisant de " + String.format("%.1f", score) + "%. Nous vous conseillons de reprendre le cours.";
        } else {
            return "❌ Résultat très insuffisant de " + String.format("%.1f", score) + "%. Une révision complète du cours est nécessaire.";
        }
    }

    private String getFallbackStrengths(double score) {
        if (score >= 60) return "Bonne compréhension des concepts fondamentaux";
        return "À développer davantage";
    }

    private String getFallbackWeaknesses(double score) {
        if (score >= 80) return "Peu d'erreurs, mais attention aux détails";
        if (score >= 60) return "Certains concepts avancés à revoir";
        return "Compréhension globale à améliorer";
    }

    private String getFallbackRecommendations(double score) {
        if (score >= 80) return "Passez au niveau supérieur avec des exercices plus complexes";
        if (score >= 60) return "Revoyez les chapitres où vous avez fait des erreurs";
        return "Reprenez le cours depuis le début et faites les exercices";
    }

    private String getFallbackGrade(double score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        if (score >= 50) return "D";
        return "F";
    }

    /**
     * Générer feedback par ID
     */
    @Transactional
    public ResultatDto generateFeedbackForResultat(Long resultatId, String language) {

        User currentUser = authService.getCurrentUser();
        Resultat resultat = resultatRepository.findById(resultatId)
                .orElseThrow(() -> new RuntimeException("Résultat non trouvé"));

        if (currentUser.getRole() == Role.ETUDIANT &&
                !resultat.getStudent().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Accès refusé");
        }

        return generateAndSaveFeedback(resultat, language);
    }

    /**
     * Obtenir résultat étudiant
     */
    public ResultatDto getResultatByStudentAndQuiz(Long quizId) {

        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        Resultat resultat = resultatRepository
                .findByStudentAndQuizId(currentUser, quiz.getId())
                .orElse(null);

        return resultat != null ? resultatMapper.toDto(resultat) : null;
    }

    /**
     * Historique étudiant
     */
    public List<ResultatDto> getResultatsByStudent() {

        User currentUser = authService.getCurrentUser();
        List<Resultat> resultats = resultatRepository
                .findByStudentOrderByCompletedDateDesc(currentUser);

        return resultats.stream()
                .map(resultatMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Résultats quiz pour enseignant
     */
    public List<ResultatDto> getResultatsByQuiz(Long quizId) {

        List<Resultat> resultats = resultatRepository
                .findByQuizIdOrderByScorePercentageDesc(quizId);

        return resultats.stream()
                .map(resultatMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Vérifier quiz terminé
     */
    public boolean hasCompletedQuiz(Long quizId) {

        User currentUser = authService.getCurrentUser();
        return resultatRepository.hasStudentCompletedQuiz(currentUser.getId(), quizId);
    }

    /**
     * Supprimer résultat
     */
    @Transactional
    public void deleteResultatByStudentAndQuiz(Long quizId) {

        User currentUser = authService.getCurrentUser();
        resultatRepository.deleteByStudentIdAndQuizId(currentUser.getId(), quizId);
    }

    // ================= STATISTICS =================

    public QuizStatisticsDto getQuizStatistics(Long quizId) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        Double averageScore = resultatRepository.getAverageScoreByQuizId(quiz.getId());
        Double bestScore = resultatRepository.getBestScoreByQuizId(quiz.getId());
        long totalStudents = resultatRepository.countByQuizIdAndStatus(quiz.getId(), Resultat.SubmissionStatus.SUBMITTED);

        return QuizStatisticsDto.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitre())
                .averageScore(averageScore != null ? averageScore : 0)
                .bestScore(bestScore != null ? bestScore : 0)
                .totalStudents((int) totalStudents)
                .ranking(getRanking(quiz))
                .build();
    }

    /**
     * Ranking
     */
    public List<RankingDto> getRanking(Quiz quiz) {

        List<Object[]> results = resultatRepository.getRankingByQuizId(quiz.getId());
        List<RankingDto> ranking = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            RankingDto dto = RankingDto.builder()
                    .rank(rank++)
                    .studentId((Long) row[0])
                    .studentName(row[1] + " " + row[2])
                    .scorePercentage((Double) row[3])
                    .earnedPoints((Integer) row[4])
                    .totalPoints((Integer) row[5])
                    .build();

            ranking.add(dto);
        }

        return ranking;
    }

    // ================= DTO CLASSES =================

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QuizStatisticsDto {
        private Long quizId;
        private String quizTitle;
        private Integer totalStudents;
        private Double averageScore;
        private Double bestScore;
        private List<RankingDto> ranking;
    }

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RankingDto {
        private Integer rank;
        private Long studentId;
        private String studentName;
        private Double scorePercentage;
        private Integer earnedPoints;
        private Integer totalPoints;
    }
}
