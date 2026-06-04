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
import com.exemple.quiz_app.quiz.repository.QuizStudentRepository;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultatService {

    private final ResultatRepository resultatRepository;
    private final QuizRepository quizRepository;
    private final QuizStudentRepository quizStudentRepository;
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
        if (Boolean.TRUE.equals(resultatRequestDto.getIsCompleted())) {

            // Recalculer le score final si nécessaire
            if (resultat.getTotalPoints() == null || resultat.getEarnedPoints() == null) {
                calculateFinalScore(resultat, student, quiz);
            }

            // Marquer comme complété (même si déjà fait)
            if (!Boolean.TRUE.equals(resultat.getIsCompleted())) {
                resultat.markAsCompleted();
                resultat.setCompletedDate(LocalDateTime.now());
                resultat.setGrade(resultat.getGradeLetter());
            } else {
                // Si déjà complété, juste mettre à jour la date si nécessaire
                if (resultat.getCompletedDate() == null) {
                    resultat.setCompletedDate(LocalDateTime.now());
                }
            }

            // Génération AI si demandé (même si déjà fait, on peut regénérer)
            if (Boolean.TRUE.equals(resultatRequestDto.getGenerateFeedback())) {
                generateAndSaveFeedback(resultat, resultatRequestDto.getLanguage() != null ?
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
        if (!Boolean.TRUE.equals(resultat.getIsCompleted())) {
            resultat.setIsCompleted(true);
            resultat.setStatus(Resultat.SubmissionStatus.SUBMITTED);
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

        if (!isRankingAvailable(quiz)) {
            throw new RuntimeException("Le classement sera disponible apres la date de fin du quiz");
        }

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
                .failedStudents(getFailedStudents(quiz))
                .build();
    }

    private List<FailedStudentDto> getFailedStudents(Quiz quiz) {
        List<User> allowedStudents = quizStudentRepository.findStudentsByQuizId(quiz.getId());
        List<Resultat> results = resultatRepository.findByQuizId(quiz.getId());
        Map<Long, Resultat> resultByStudentId = new HashMap<>();

        for (Resultat result : results) {
            if (result.getStudent() != null && result.getStudent().getId() != null) {
                resultByStudentId.put(result.getStudent().getId(), result);
            }
        }

        List<FailedStudentDto> failedStudents = new ArrayList<>();
        Set<Long> handledStudentIds = new HashSet<>();

        for (User student : allowedStudents) {
            if (student == null || student.getId() == null) {
                continue;
            }

            handledStudentIds.add(student.getId());
            Resultat result = resultByStudentId.get(student.getId());

            if (result == null) {
                failedStudents.add(buildFailedStudentDto(quiz, student, null, "NON_REPONDU", "Non repondu"));
            } else if (!isSubmitted(result)) {
                failedStudents.add(buildFailedStudentDto(quiz, student, result, "NON_TERMINE", "Non termine"));
            } else if (isFailedResult(result)) {
                failedStudents.add(buildFailedStudentDto(quiz, student, result, "NOTE_INSUFFISANTE", "Note insuffisante"));
            }
        }

        for (Resultat result : results) {
            User student = result.getStudent();
            Long studentId = student != null ? student.getId() : null;

            if (studentId != null && handledStudentIds.contains(studentId)) {
                continue;
            }

            if (!isSubmitted(result)) {
                failedStudents.add(buildFailedStudentDto(quiz, student, result, "NON_TERMINE", "Non termine"));
            } else if (isFailedResult(result)) {
                failedStudents.add(buildFailedStudentDto(quiz, student, result, "NOTE_INSUFFISANTE", "Note insuffisante"));
            }
        }

        failedStudents.sort((a, b) -> {
            int quizCompare = safeString(a.getQuizTitle()).compareToIgnoreCase(safeString(b.getQuizTitle()));
            if (quizCompare != 0) return quizCompare;

            int classCompare = safeString(a.getClassName()).compareToIgnoreCase(safeString(b.getClassName()));
            if (classCompare != 0) return classCompare;

            return safeString(a.getStudentName()).compareToIgnoreCase(safeString(b.getStudentName()));
        });

        return failedStudents;
    }

    private FailedStudentDto buildFailedStudentDto(
            Quiz quiz,
            User student,
            Resultat result,
            String failureReason,
            String statusLabel
    ) {
        Double scorePercentage = result != null ? getScorePercentage(result) : 0.0;
        Double noteSur20 = result != null ? getNoteSur20(result) : 0.0;
        Long classId = getClassId(quiz, student);
        String className = getClassName(quiz, student);

        return FailedStudentDto.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitre())
                .subjectName(quiz.getTheme())
                .matiereName(quiz.getTheme())
                .studentId(student != null ? student.getId() : null)
                .studentName(getStudentName(student))
                .studentFirstName(student != null ? student.getFirstName() : null)
                .studentLastName(student != null ? student.getLastName() : null)
                .studentEmail(student != null ? student.getEmail() : null)
                .cne(student != null ? student.getCne() : null)
                .codeApoge(student != null ? student.getCodeApoge() : null)
                .classId(classId)
                .classeId(classId)
                .className(className)
                .classeName(className)
                .scorePercentage(scorePercentage)
                .noteSur20(noteSur20)
                .earnedPoints(result != null ? result.getEarnedPoints() : 0)
                .totalPoints(result != null ? result.getTotalPoints() : null)
                .failureReason(failureReason)
                .statusLabel(statusLabel)
                .completedDate(result != null ? result.getCompletedDate() : null)
                .availableUntil(quiz.getAvailableUntil())
                .build();
    }

    private boolean isSubmitted(Resultat result) {
        return result != null && Resultat.SubmissionStatus.SUBMITTED.equals(result.getStatus());
    }

    private boolean isFailedResult(Resultat result) {
        String grade = result.getGrade() != null ? result.getGrade().trim().toUpperCase() : "";
        return "F".equals(grade) || getScorePercentage(result) < 50.0;
    }

    private Double getScorePercentage(Resultat result) {
        if (result == null) return 0.0;
        if (result.getScorePercentage() != null) return result.getScorePercentage();

        Integer earned = result.getEarnedPoints();
        Integer total = result.getTotalPoints();
        if (earned != null && total != null && total > 0) {
            return (earned * 100.0) / total;
        }

        return result.getScore() != null ? result.getScore() : 0.0;
    }

    private Double getNoteSur20(Resultat result) {
        Integer earned = result != null ? result.getEarnedPoints() : null;
        Integer total = result != null ? result.getTotalPoints() : null;

        if (earned != null && total != null && total > 0) {
            return (earned * 20.0) / total;
        }

        return (getScorePercentage(result) * 20.0) / 100.0;
    }

    private Long getClassId(Quiz quiz, User student) {
        if (student != null && student.getClasse() != null) {
            return student.getClasse().getId();
        }
        return quiz.getClasse() != null ? quiz.getClasse().getId() : null;
    }

    private String getClassName(Quiz quiz, User student) {
        if (student != null && student.getClasse() != null) {
            return student.getClasse().getName();
        }
        return quiz.getClasse() != null ? quiz.getClasse().getName() : "Classe non definie";
    }

    private String getStudentName(User student) {
        if (student == null) return "Etudiant";
        return (safeString(student.getFirstName()) + " " + safeString(student.getLastName())).trim();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    /**
     * Ranking
     */
    public List<RankingDto> getRanking(Quiz quiz) {

        List<Resultat> results = resultatRepository.findByQuizIdOrderByScorePercentageDesc(quiz.getId());
        List<RankingDto> ranking = new ArrayList<>();
        int rank = 1;

        for (Resultat resultat : results) {
            User student = resultat.getStudent();
            String className = null;
            Long classId = null;

            if (student != null && student.getClasse() != null) {
                classId = student.getClasse().getId();
                className = student.getClasse().getName();
            } else if (quiz.getClasse() != null) {
                classId = quiz.getClasse().getId();
                className = quiz.getClasse().getName();
            }

            RankingDto dto = RankingDto.builder()
                    .rank(rank++)
                    .studentId(student != null ? student.getId() : null)
                    .studentName(student != null ? student.getFullName() : "")
                    .studentFirstName(student != null ? student.getFirstName() : null)
                    .studentLastName(student != null ? student.getLastName() : null)
                    .studentEmail(student != null ? student.getEmail() : null)
                    .cne(student != null ? student.getCne() : null)
                    .codeApoge(student != null ? student.getCodeApoge() : null)
                    .classId(classId)
                    .classeId(classId)
                    .className(className)
                    .classeName(className)
                    .subjectName(quiz.getTheme())
                    .matiereName(quiz.getTheme())
                    .scorePercentage(resultat.getScorePercentage())
                    .earnedPoints(resultat.getEarnedPoints())
                    .totalPoints(resultat.getTotalPoints())
                    .build();

            ranking.add(dto);
        }

        return ranking;
    }

    private boolean isRankingAvailable(Quiz quiz) {
        if (quiz.getStatus() == Quiz.QuizStatus.EXPIRED) {
            return true;
        }

        LocalDateTime availableUntil = quiz.getAvailableUntil();
        return availableUntil != null && !availableUntil.isAfter(LocalDateTime.now());
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
        private List<FailedStudentDto> failedStudents;
    }

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RankingDto {
        private Integer rank;
        private Long studentId;
        private String studentName;
        private String studentFirstName;
        private String studentLastName;
        private String studentEmail;
        private String cne;
        private String codeApoge;
        private Long classId;
        private Long classeId;
        private String className;
        private String classeName;
        private String subjectName;
        private String matiereName;
        private Double scorePercentage;
        private Integer earnedPoints;
        private Integer totalPoints;
    }

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FailedStudentDto {
        private Long quizId;
        private String quizTitle;
        private String subjectName;
        private String matiereName;
        private Long studentId;
        private String studentName;
        private String studentFirstName;
        private String studentLastName;
        private String studentEmail;
        private String cne;
        private String codeApoge;
        private Long classId;
        private Long classeId;
        private String className;
        private String classeName;
        private Double scorePercentage;
        private Double noteSur20;
        private Integer earnedPoints;
        private Integer totalPoints;
        private String failureReason;
        private String statusLabel;
        private LocalDateTime completedDate;
        private LocalDateTime availableUntil;
    }
}
