package com.exemple.quiz_app.classe.dto;

import lombok.Data;

@Data
public class StudentRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String cne;
    private String codeApoge;
}
