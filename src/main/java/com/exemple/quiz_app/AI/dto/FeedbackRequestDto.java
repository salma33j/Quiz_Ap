package com.exemple.quiz_app.AI.dto;

import lombok.Data;
import java.util.List;

@Data
public class FeedbackRequestDto {

    // Informations de base
    private Long resultatId;
    private String studentName;
    private String quizTitle;
    private String quizTheme;
    private Double score;
    private Integer totalPoints;
    private Integer earnedPoints;
    private String language;  // "fr" ou "en"

    // 🔥 Détail des questions et réponses
    private List<QuestionFeedbackDto> questions;

    @Data
    public static class QuestionFeedbackDto {
        private Long questionId;
        private String questionText;
        private String studentAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private Integer points;
        private String topic;  // Optionnel: thème de la question
    }
}