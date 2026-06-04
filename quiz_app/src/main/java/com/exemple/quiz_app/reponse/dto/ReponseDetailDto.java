package com.exemple.quiz_app.reponse.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReponseDetailDto {
    private Long questionId;
    private String questionText;
    private String studentAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Integer pointsEarned;
    private Integer pointsMax;
    private String explanation;
    private List<String> options;
}