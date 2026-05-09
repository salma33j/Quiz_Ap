package com.exemple.quiz_app.reponse.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReponseDto {
    private Long id;
    private Long quizId;
    private String quizTitle;
    private Long questionId;
    private String questionText;
    private String questionType;
    private String studentAnswer;
    private String correctAnswer;
    private Boolean isCorrect;
    private Integer pointsEarned;
    private Integer pointsMax;
    private LocalDateTime answeredAt;
    private String feedback;
}