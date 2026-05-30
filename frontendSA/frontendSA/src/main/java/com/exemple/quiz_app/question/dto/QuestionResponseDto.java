package com.exemple.quiz_app.question.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionResponseDto {
    private Long id;
    private String enonce;
    private List<String> options;
    private Integer points;
    private String type;
    private LocalDateTime createdAt;
}