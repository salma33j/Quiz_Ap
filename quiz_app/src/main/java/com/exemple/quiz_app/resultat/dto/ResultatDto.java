package com.exemple.quiz_app.resultat.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultatDto {
    private Long id;
    private Long quizId;
    private String quizTitle;
    private String quizTheme;
    private Long studentId;
    private String studentName;
    private String studentFirstName;
    private Double score;
    private Integer totalPoints;
    private Integer earnedPoints;
    private Double scorePercentage;
    private Boolean isCompleted;
    private String feedbackIa;
    private String strengths;
    private String weaknesses;
    private String recommendations;
    private String suggestedQuiz;
    private String grade;
    private String cne;
    private String codeApogee;
    private String className;
    private String studentLastName;
    private String subjectName;
    private LocalDateTime startedAt;
    private LocalDateTime completedDate;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Double noteSur20;
    private String mention;
    // Méthodes utilitaires
    public String getFormattedScore() {
        return String.format("%.1f%%", scorePercentage);
    }
    public String getLetterGrade() {
        if (scorePercentage == null) return "N/A";
        if (scorePercentage >= 90) return "A+";
        if (scorePercentage >= 80) return "A";
        if (scorePercentage >= 70) return "B";
        if (scorePercentage >= 60) return "C";
        if (scorePercentage >= 50) return "D";
        return "F";
    }
}
