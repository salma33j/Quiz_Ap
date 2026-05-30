package com.exemple.quiz_app.quiz.dto;

import lombok.Data;
import java.util.List;

@Data
public class StudentListDto {
    private List<StudentInfo> students;

    @Data
    public static class StudentInfo {
        private String nom;
        private String prenom;
        private String email;
        private String cne;
        private String codeApoge;
        private String classe;
        private String filiere;
    }
}
