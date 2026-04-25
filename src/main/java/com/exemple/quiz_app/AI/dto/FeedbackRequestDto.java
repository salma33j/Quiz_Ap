package com.exemple.quiz_app.AI.dto;

import lombok.Data;

@Data
public class FeedbackRequestDto {
    private Long resultatId;
    private String language;  // "fr" ou "en"
}