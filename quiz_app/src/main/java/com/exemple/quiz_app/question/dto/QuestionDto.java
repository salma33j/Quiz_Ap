package com.exemple.quiz_app.question.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionDto {
    private Long id;
    private String enonce;
    private List<String> options;
    private String type;
    private Integer points;
}