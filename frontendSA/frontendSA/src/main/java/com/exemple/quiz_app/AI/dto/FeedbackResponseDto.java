package com.exemple.quiz_app.AI.dto;

import lombok.Data;

@Data
public class FeedbackResponseDto {
    private String feedback;
    private String strengths;
    private String weaknesses;
    private String recommendations;
    private String suggestedQuiz;
    private Double score;
    private String grade;
}