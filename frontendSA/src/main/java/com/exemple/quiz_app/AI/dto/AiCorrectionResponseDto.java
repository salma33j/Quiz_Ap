package com.exemple.quiz_app.AI.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCorrectionResponseDto {
    private Boolean isCorrect;
    private Integer pointsEarned;
    private String feedback;
    private String explanation;
    private Double similarityScore;
}