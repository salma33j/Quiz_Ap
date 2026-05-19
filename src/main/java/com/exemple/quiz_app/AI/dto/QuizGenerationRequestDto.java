package com.exemple.quiz_app.AI.dto;

import lombok.Data;

@Data
public class QuizGenerationRequestDto {
    private String theme;
    private Integer numberOfQuestions;
    private String difficulty;  // "FACILE", "MOYEN", "DIFFICILE"
    private String type;        // "QCM", "TRUE_FALSE", "TEXT", "ALL"
    private Long quizId;
}
