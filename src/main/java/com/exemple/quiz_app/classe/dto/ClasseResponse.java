package com.exemple.quiz_app.classe.dto;

import lombok.Data;
import java.util.List;

@Data
public class ClasseResponse {
    private Long id;
    private String name;
    private String filiere;
    private String niveau;
    private Long studentCount;
    private Long enseignantId;
    private String enseignantName;
    private String enseignantEmail;
    private List<Long> enseignantIds;
    private List<String> enseignantNames;
}
