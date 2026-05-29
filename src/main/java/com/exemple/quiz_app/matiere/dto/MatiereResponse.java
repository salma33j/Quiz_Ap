package com.exemple.quiz_app.matiere.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatiereResponse {
    private Long id;
    private String nom;
    private String description;
    private Long classId;
    private String className;
    private String classFiliere;
    private String classNiveau;
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    private LocalDateTime createdAt;
}
