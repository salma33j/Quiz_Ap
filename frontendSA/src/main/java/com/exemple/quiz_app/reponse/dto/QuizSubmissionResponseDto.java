package com.exemple.quiz_app.reponse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionResponseDto {
    private Double score;
    private Integer earnedPoints;
    private Integer totalPoints;
    private Double percentage;
    private String grade;
    private String feedback;
    private String recommendations;
    private String strengths;
    private String weaknesses;
    private Boolean isCompleted;
}