package com.exemple.quiz_app.reponse.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class ReponseRequestDto {
    @NotNull(message = "L'ID du quiz est requis")
    private Long quizId;
    @NotNull(message = "L'ID de la question est requis")
    private Long questionId;
    @NotNull(message = "La réponse est requise")
    private String studentAnswer;
}