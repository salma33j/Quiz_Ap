package com.exemple.quiz_app.classe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseDto {
    private Long id;
    private String name;
    private String filiere;
    private String niveau;
    private Integer studentCount;
    private List<Long> enseignantIds;
    private List<String> enseignantNames;
}
