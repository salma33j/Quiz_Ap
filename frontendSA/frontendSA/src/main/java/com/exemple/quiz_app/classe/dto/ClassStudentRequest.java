package com.exemple.quiz_app.classe.dto;

import lombok.Data;

@Data
public class ClassStudentRequest {
    private Long studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String cne;
    private String codeApoge;
}
