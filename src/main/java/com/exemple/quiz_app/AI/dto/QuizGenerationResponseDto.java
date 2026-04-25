package com.exemple.quiz_app.AI.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuizGenerationResponseDto {
    private String theme;
    private Integer numberOfQuestions;
    private List<GeneratedQuestionDto> questions;

    @Data
    public static class GeneratedQuestionDto {
        private String questionText;
        private String type;           // "MCQ", "TRUE_FALSE", "TEXT"
        private List<String> options;
        private String correctAnswer;
        private Integer points;
    }
}