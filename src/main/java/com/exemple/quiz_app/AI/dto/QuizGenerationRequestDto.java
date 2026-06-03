package com.exemple.quiz_app.AI.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizGenerationRequestDto {
    @NotBlank(message = "La matiere est obligatoire.")
    private String matiere;

    @NotBlank(message = "Le titre est obligatoire.")
    private String titre;

    @NotBlank(message = "La description est obligatoire.")
    private String description;

    private String theme;

    private String classe;

    @NotNull(message = "Le nombre de questions est obligatoire.")
    @Min(value = 1, message = "Le nombre de questions doit etre superieur a 0.")
    private Integer numberOfQuestions;

    @NotBlank(message = "La difficulte est obligatoire.")
    private String difficulty;  // "FACILE", "MOYEN", "DIFFICILE"

    private String type;        // "QCM", "TRUE_FALSE", "TEXT", "ALL"
    private Long quizId;
}
