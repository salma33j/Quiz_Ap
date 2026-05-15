package com.exemple.quiz_app.reponse.dto;


import com.exemple.quiz_app.resultat.dto.ResultatDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionFinalResponseDto {
    private ResultatDto resultat;
    private List<ReponseDto> reponses;
    private Boolean isCompleted;
}