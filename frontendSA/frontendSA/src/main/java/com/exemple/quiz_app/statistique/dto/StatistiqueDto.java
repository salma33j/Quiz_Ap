package com.exemple.quiz_app.statistique.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiqueDto {

    // Informations générales
    private Long quizId;
    private String quizTitle;
    private String quizTheme;
    private Long classId;
    private Long classeId;
    private String className;
    private String classeName;
    private String subjectName;
    private String matiereName;
    private String enseignantNom;
    private LocalDateTime periodeDebut;
    private LocalDateTime periodeFin;

    // Statistiques de participation
    private Integer totalParticipants;
    private Integer totalStudentsAllowed;
    private Integer totalQuestions;

    // Statistiques de scores
    private Double moyenneScore;
    private Double meilleurScore;
    private Double pireScore;
    private Double medianeScore;

    // Distribution des scores
    private ScoreDistributionDto scoreDistribution;

    // Classement
    private List<StudentStatDto> classement;
    private List<StudentStatDto> top5;
    private List<StudentStatDto> bottom5;

    // Statistiques par question
    private List<QuestionStatDto> questionsStats;

    // Statistiques de temps
    private Double tempsMoyenReponse;
    private Double tempsMinReponse;
    private Double tempsMaxReponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDistributionDto {
        private int excellent;
        private int tresBien;
        private int bien;
        private int assezBien;
        private int moyen;
        private int insuffisant;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentStatDto {
        private Long studentId;
        private String studentNom;
        private String studentPrenom;
        private String studentEmail;
        private String cne;
        private String codeApoge;
        private Long classId;
        private Long classeId;
        private String className;
        private String classeName;
        private Double scorePourcentage;
        private Integer earnedPoints;
        private Integer totalPoints;
        private Integer rang;
        private String grade;
        private LocalDateTime completedDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionStatDto {
        private Long questionId;
        private String questionText;
        private Integer points;
        private Integer totalReponses;
        private Integer reponsesCorrectes;
        private Double tauxReussite;
        private Map<String, Integer> reponsesDistribution;
    }
}
