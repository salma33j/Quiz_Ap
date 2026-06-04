package com.exemple.quiz_app.AI.dto;

import lombok.Data;

@Data
public class FeedbackRequestDto {

    // ================= INFORMATIONS PRINCIPALES =================

    private Long resultatId;

    private String studentName;

    private String quizTitle;

    private String quizTheme;

    // 🔥 Score final uniquement
    private Double score;

    private Integer totalPoints;

    private Integer earnedPoints;

    // fr / en
    private String language;

    // 🔥 Niveau calculé localement dans Spring Boot
    // excellent / good / average / weak
    private String performanceLevel;

}