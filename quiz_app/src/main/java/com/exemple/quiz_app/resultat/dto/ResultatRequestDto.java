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
public class ResultatRequestDto {
    private Long quizId;
    private Long studentId;
    private Double score;
    private Integer totalPoints;
    private Integer earnedPoints;
    private Double scorePercentage;
    private Boolean isCompleted;
    private LocalDateTime completedDate;
    private Boolean generateFeedback;  // Si true, génère automatiquement un feedback IA
    private String language;  // "fr" ou "en" pour le feedback IA
}