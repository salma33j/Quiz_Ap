package com.exemple.quiz_app.quiz.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuizForStudentDto {
    private Long id;
    private String titre;
    private String theme;
    private Long matiereId;
    private String matiereName;
    private String matiereNom;
    private Long classId;
    private Long classeId;
    private String className;
    private String classeName;
    private String classFiliere;
    private String classNiveau;
    private String enseignantNom;
    private Integer questionCount;
    private Integer timeLimit;
    private LocalDateTime availableUntil;
    private String status;
    private Long timeRemainingSeconds;
}
