package com.exemple.quiz_app.matiere.dto;

import lombok.Data;

@Data
public class MatiereRequest {
    private String nom;
    private String description;
    private Long classId;
    private Long teacherId;
}
