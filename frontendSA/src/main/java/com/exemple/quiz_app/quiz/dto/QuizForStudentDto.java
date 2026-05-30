package com.exemple.quiz_app.quiz.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuizForStudentDto {
    private Long id;
    private String titre;
    private String theme;
    private String enseignantNom;
    private Integer questionCount;
    private Integer timeLimit;
    private LocalDateTime availableUntil;
    private String status;
    private Long timeRemainingSeconds;
}