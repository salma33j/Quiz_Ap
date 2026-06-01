package com.exemple.quiz_app.classe.dto;

import lombok.Data;

@Data
public class StudentResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String cne;
    private String codeApoge;
    private Long classId;
    private String className;
    private String classFiliere;
    private String classNiveau;
}
