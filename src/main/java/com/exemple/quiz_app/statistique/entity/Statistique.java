package com.exemple.quiz_app.statistique.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entité non persistée (DTO métier) pour les statistiques
 * Les statistiques sont calculées à la volée à partir des données existantes
 * (Quiz, Resultat, Question, Reponse)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Statistique {

    // ========== INFORMATIONS GENERALES ==========
    private Long quizId;
    private String quizTitle;
    private String quizTheme;
    private String enseignantNom;
    private LocalDateTime periodeDebut;
    private LocalDateTime periodeFin;

    // ========== STATISTIQUES DE PARTICIPATION ==========
    private Integer totalParticipants;
    private Integer totalStudentsAllowed;
    private Integer totalQuestions;

    // ========== STATISTIQUES DE SCORES ==========
    private Double moyenneScore;
    private Double meilleurScore;
    private Double pireScore;
    private Double medianeScore;
    private Double ecartType;

    // ========== DISTRIBUTION DES SCORES ==========
    private ScoreDistribution scoreDistribution;

    // ========== CLASSEMENTS ==========
    private List<StudentStat> classement;
    private List<StudentStat> meilleursEtudiants;
    private List<StudentStat> plusMauvaisEtudiants;

    // ========== STATISTIQUES PAR QUESTION ==========
    private List<QuestionStat> questionsStats;

    // ========== STATISTIQUES DE TEMPS ==========
    private Double tempsMoyenReponse;  // en secondes
    private Double tempsMinReponse;
    private Double tempsMaxReponse;

    // ========== CLASSES INTERNES ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDistribution {
        private int excellent;      // >= 90%
        private int tresBien;       // >= 80%
        private int bien;           // >= 70%
        private int assezBien;      // >= 60%
        private int moyen;          // >= 50%
        private int insuffisant;    // < 50%
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentStat {
        private Long studentId;
        private String studentNom;
        private String studentPrenom;
        private String studentEmail;
        private Double score;
        private Double scorePercentage;
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
    public static class QuestionStat {
        private Long questionId;
        private String questionText;
        private Integer points;
        private Integer totalReponses;
        private Integer reponsesCorrectes;
        private Double tauxReussite;
        private Map<String, Integer> reponsesDistribution;  // Pour les QCM
        private List<AnswerDistribution> answerDistribution; // Alternative plus détaillée
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDistribution {
        private String answerText;
        private Integer count;
        private Double percentage;
        private Boolean isCorrect;
    }
}