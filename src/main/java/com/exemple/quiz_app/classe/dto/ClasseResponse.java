package com.exemple.quiz_app.classe.dto;

import lombok.Data;

@Data
public class ClasseResponse {
    private Long id;
    private String name;
    private String filiere;
    private String niveau;
    private Long studentCount;
}