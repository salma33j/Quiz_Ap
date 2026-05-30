package com.exemple.quiz_app.classe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudentDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String cne;
    private String codeApoge;
    private Long classId;
    private String className;
}
