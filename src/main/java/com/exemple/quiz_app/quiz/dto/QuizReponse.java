package com.exemple.quiz_app.quiz.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuizReponse {
    private Long id;
    private String titre;
    private String theme;
    private Integer questionCount;
    private LocalDateTime availableFrom;
    private LocalDateTime availableUntil;
    private Integer timeLimit;
    private String status;
    private String creationType;
    private String enseignantNom;
    private Integer totalStudentsAllowed;
    private LocalDateTime createdAt;
}