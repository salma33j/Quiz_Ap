package com.exemple.quiz_app.classe.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClasseRequest {
    private String name;
    private String filiere;
    private String niveau;
    private List<Long> teacherIds;
}
