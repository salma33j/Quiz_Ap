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

    @Transactional
    public ResultatDto saveOrUpdateResultat(ResultatRequestDto resultatRequestDto) {
        User currentUser = authService.getCurrentUser();

        User student;
        if (resultatRequestDto.getStudentId() != null &&
                (currentUser.getRole() == Role.ENSEIGNANT || currentUser.getRole() == Role.ADMIN)) {
            throw new UnsupportedOperationException("À implémenter avec le service User");
        } else {
            student = currentUser;
        }

        if (student.getRole() != Role.ETUDIANT && student.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seuls les étudiants peuvent avoir des résultats");
        }

        Quiz quiz = quizRepository.findById(resultatRequestDto.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (quiz.isDeleted()) {
            throw new RuntimeException("Ce quiz n'est plus disponible");
        }

        Resultat resultat = resultatRepository.findByStudentAndQuizId(student, quiz.getId())
                .orElse(null);

        if (resultat == null) {
            resultat = resultatMapper.toEntity(resultatRequestDto, quiz, student);
        } else {
            resultatMapper.updateEntity(resultatRequestDto, resultat);
        }

        if (resultatRequestDto.getIsCompleted() != null && resultatRequestDto.getIsCompleted() &&
                (resultat.getIsCompleted() == null || !resultat.getIsCompleted())) {
            calculateFinalScore(resultat, student, quiz);
            resultat.markAsCompleted();

            if (resultatRequestDto.getGenerateFeedback() != null && resultatRequestDto.getGenerateFeedback()) {
                generateAndSaveFeedback(resultat, resultatRequestDto.getLanguage());
            }
        }

        Resultat savedResultat = resultatRepository.save(resultat);
        return resultatMapper.toDto(savedResultat);
    }

    private void calculateFinalScore(Resultat resultat, User student, Quiz quiz) {
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        List<Reponse> reponses = reponseRepository.findByStudentAndQuestionQuizId(student, quiz.getId());

        int totalPointsPossible = questions.stream().mapToInt(Question::getPoints).sum();
        int earnedPoints = reponses.stream().mapToInt(r -> r.getPointsEarned() != null ? r.getPointsEarned() : 0).sum();
        double scorePercentage = totalPointsPossible > 0 ? (earnedPoints * 100.0) / totalPointsPossible : 0;

        resultat.setTotalPoints(totalPointsPossible);
        resultat.setEarnedPoints(earnedPoints);
        resultat.setScore((double) earnedPoints);
        resultat.setScorePercentage(scorePercentage);
        resultat.setIsCompleted(true);
        resultat.setCompletedDate(LocalDateTime.now());
        resultat.setGrade(resultat.getGradeLetter());
    }

    @Transactional
    public ResultatDto generateAndSaveFeedback(Resultat resultat, String language) {
        if (resultat == null || !resultat.getIsCompleted()) {
            throw new RuntimeException("Le résultat doit être complété avant de générer un feedback");
        }

        Quiz quiz = resultat.getQuiz();
        User student = resultat.getStudent();

        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        List<Reponse> reponses = reponseRepository.findByStudentAndQuestionQuizId(student, quiz.getId());

        FeedbackRequestDto feedbackRequest = new FeedbackRequestDto();
        feedbackRequest.setResultatId(resultat.getId());
        feedbackRequest.setStudentName(student.getNom());
        feedbackRequest.setQuizTitle(quiz.getTitre());
        feedbackRequest.setQuizTheme(quiz.getTheme());
        feedbackRequest.setScore(resultat.getScorePercentage());
        feedbackRequest.setTotalPoints(resultat.getTotalPoints());
        feedbackRequest.setEarnedPoints(resultat.getEarnedPoints());
        feedbackRequest.setLanguage(language != null ? language : "fr");

        List<FeedbackRequestDto.QuestionFeedbackDto> questionDetails = new ArrayList<>();
        for (Question q : questions) {
            Reponse r = reponses.stream()
                    .filter(rep -> rep.getQuestion().getId().equals(q.getId()))
                    .findFirst()
                    .orElse(null);

            FeedbackRequestDto.QuestionFeedbackDto qDto = new FeedbackRequestDto.QuestionFeedbackDto();
            qDto.setQuestionId(q.getId());
            qDto.setQuestionText(q.getEnonce());
            qDto.setStudentAnswer(r != null ? r.getStudentAnswer() : "Non répondue");
            qDto.setCorrectAnswer(q.getCorrectAnswerText());
            qDto.setIsCorrect(r != null && r.getIsCorrect());
            qDto.setPoints(q.getPoints());
            qDto.setTopic(quiz.getTheme());
            questionDetails.add(qDto);
        }
        feedbackRequest.setQuestions(questionDetails);

        FeedbackResponseDto feedbackResponse = aiFeedbackService.generateFeedback(feedbackRequest);

        resultat.setFeedbackIa(feedbackResponse.getFeedback());
        resultat.setStrengths(feedbackResponse.getStrengths());
        resultat.setWeaknesses(feedbackResponse.getWeaknesses());
        resultat.setRecommendations(feedbackResponse.getRecommendations());
        resultat.setSuggestedQuiz(feedbackResponse.getSuggestedQuiz());
        resultat.setGrade(feedbackResponse.getGrade());

        resultatRepository.save(resultat);
        return resultatMapper.toDto(resultat);
    }

    @Transactional
    public ResultatDto generateFeedbackForResultat(Long resultatId, String language) {
        User currentUser = authService.getCurrentUser();
        Resultat resultat = resultatRepository.findById(resultatId)
                .orElseThrow(() -> new RuntimeException("Résultat non trouvé"));

        if (currentUser.getRole() == Role.ETUDIANT && !resultat.getStudent().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous ne pouvez pas accéder au feedback d'un autre étudiant");
        }

        if (currentUser.getRole() == Role.ENSEIGNANT && !resultat.getQuiz().getEnseignant().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'êtes pas l'enseignant de ce quiz");
        }

        return generateAndSaveFeedback(resultat, language);
    }

    public ResultatDto getResultatByStudentAndQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        Resultat resultat = resultatRepository.findByStudentAndQuizId(currentUser, quiz.getId())
                .orElse(null);

        return resultat != null ? resultatMapper.toDto(resultat) : null;
    }

    /**
     * Obtenir tous les résultats d'un étudiant (historique)
     */
    public List<ResultatDto> getResultatsByStudent() {
        User currentUser = authService.getCurrentUser();
        // 🔥 CORRIGÉ: utiliser findByStudentOrderByCompletedDateDesc
        List<Resultat> resultats = resultatRepository.findByStudentOrderByCompletedDateDesc(currentUser);
        return resultats.stream()
                .map(resultatMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ResultatDto> getResultatsByQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (currentUser.getRole() != Role.ADMIN &&
                !quiz.getEnseignant().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à voir les résultats de ce quiz");
        }

        List<Resultat> resultats = resultatRepository.findByQuizIdOrderByScorePercentageDesc(quiz.getId());
        return resultats.stream()
                .map(resultatMapper::toDto)
                .collect(Collectors.toList());
    }

    public QuizStatisticsDto getQuizStatistics(Long quizId) {
        User currentUser = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz non trouvé"));

        if (currentUser.getRole() != Role.ADMIN &&
                !quiz.getEnseignant().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à voir les statistiques de ce quiz");
        }

        Double averageScore = resultatRepository.getAverageScoreByQuizId(quiz.getId());
        Double bestScore = resultatRepository.getBestScoreByQuizId(quiz.getId());
        long totalStudents = resultatRepository.countByQuizIdAndStatus(quiz.getId(), Resultat.SubmissionStatus.SUBMITTED);

        ScoreDistribution distribution = getScoreDistribution(quiz);
        List<QuestionStatsDto> questionStats = getQuestionStatistics(quiz);

        return QuizStatisticsDto.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitre())
                .totalStudents((int) totalStudents)
                .averageScore(averageScore != null ? averageScore : 0)
                .bestScore(bestScore != null ? bestScore : 0)
                .scoreDistribution(distribution)
                .questionStatistics(questionStats)
                .ranking(getRanking(quiz))
                .build();
    }

    /**
     * Obtenir la distribution des scores
     */
    private ScoreDistribution getScoreDistribution(Quiz quiz) {
        // 🔥 CORRIGÉ: utiliser findByQuizIdOrderByScorePercentageDesc
        List<Resultat> resultats = resultatRepository.findByQuizIdOrderByScorePercentageDesc(quiz.getId());

        int excellent = 0, tresBien = 0, bien = 0, assezBien = 0, moyen = 0, insuffisant = 0;

        for (Resultat r : resultats) {
            double score = r.getScorePercentage() != null ? r.getScorePercentage() : 0;
            if (score >= 90) excellent++;
            else if (score >= 80) tresBien++;
            else if (score >= 70) bien++;
            else if (score >= 60) assezBien++;
            else if (score >= 50) moyen++;
            else insuffisant++;
        }

        return new ScoreDistribution(excellent, tresBien, bien, assezBien, moyen, insuffisant);
    }

    private List<QuestionStatsDto> getQuestionStatistics(Quiz quiz) {
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        List<Resultat> resultats = resultatRepository.findByQuizIdOrderByScorePercentageDesc(quiz.getId());
        List<QuestionStatsDto> stats = new ArrayList<>();

        for (Question q : questions) {
            long totalReponses = 0;
            long correctReponses = 0;

            for (Resultat r : resultats) {
                List<Reponse> reponses = reponseRepository.findByStudentAndQuestionQuizId(r.getStudent(), quiz.getId());
                Reponse reponse = reponses.stream()
                        .filter(rep -> rep.getQuestion().getId().equals(q.getId()))
                        .findFirst()
                        .orElse(null);

                if (reponse != null) {
                    totalReponses++;
                    if (Boolean.TRUE.equals(reponse.getIsCorrect())) {
                        correctReponses++;
                    }
                }
            }

            double successRate = totalReponses > 0 ? (correctReponses * 100.0) / totalReponses : 0;
            stats.add(QuestionStatsDto.builder()
                    .questionId(q.getId())
                    .questionText(q.getEnonce())
                    .totalResponses((int) totalReponses)
                    .correctResponses((int) correctReponses)
                    .successRate(successRate)
                    .build());
        }
        return stats;
    }

    public List<RankingDto> getRanking(Quiz quiz) {
        List<Object[]> results = resultatRepository.getRankingByQuizId(quiz.getId());
        List<RankingDto> ranking = new ArrayList<>();
        int rank = 1;

        for (Object[] row : results) {
            RankingDto dto = RankingDto.builder()
                    .rank(rank++)
                    .studentId((Long) row[0])
                    .studentName(row[1] + " " + row[2])
                    .scorePercentage((Double) row[4])
                    .earnedPoints((Integer) row[5])
                    .totalPoints((Integer) row[6])
                    .build();
            ranking.add(dto);
        }
        return ranking;
    }

    @Transactional
    public void deleteResultatByStudentAndQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        resultatRepository.deleteByStudentIdAndQuizId(currentUser.getId().longValue(), quizId);
    }

    public boolean hasCompletedQuiz(Long quizId) {
        User currentUser = authService.getCurrentUser();
        return resultatRepository.hasStudentCompletedQuiz(currentUser.getId().longValue(), quizId);
    }

    // ========== CLASSES INTERNES POUR LES STATISTIQUES ==========

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
        private ScoreDistribution scoreDistribution;
        private List<QuestionStatsDto> questionStatistics;
        private List<RankingDto> ranking;
    }

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScoreDistribution {
        private int excellent;
        private int tresBien;
        private int bien;
        private int assezBien;
        private int moyen;
        private int insuffisant;
    }

    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QuestionStatsDto {
        private Long questionId;
        private String questionText;
        private Integer totalResponses;
        private Integer correctResponses;
        private Double successRate;
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