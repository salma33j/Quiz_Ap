package com.exemple.quiz_app.quiz.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuizRequest {
    private String titre;
    private String theme;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer timeLimit;
    private String creationType;  // "MANUAL" ou "AI"
    private Long classeId;
    private Long matiereId;
}