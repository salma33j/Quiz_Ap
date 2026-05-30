package com.exemple.quiz_app.AI.dto;

import lombok.Data;

@Data
public class QuizGenerationRequestDto {
    private String theme;
    private Integer numberOfQuestions;
    private String difficulty;  // "FACILE", "MOYEN", "DIFFICILE"
    private Long quizId;
}