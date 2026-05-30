package com.exemple.quiz_app.AI.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCorrectionRequestDto {
    private String questionText;
    private String expectedAnswer;
    private String studentAnswer;
    private Integer pointsMax;
    private String language; // "fr" ou "en"
}