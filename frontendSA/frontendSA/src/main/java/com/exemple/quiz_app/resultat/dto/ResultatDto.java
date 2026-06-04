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
    private LocalDateTime startedAt;
    private LocalDateTime completedDate;
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
