package com.exemple.quiz_app.question.dto;

import lombok.Data;

@Data
public class QuestionRequestDto {
    private String enonce;
    private String choixA;
    private String choixB;
    private String choixC;
    private String choixD;
    private String reponseCorrecte;
    private Integer points;
    private String type;
}