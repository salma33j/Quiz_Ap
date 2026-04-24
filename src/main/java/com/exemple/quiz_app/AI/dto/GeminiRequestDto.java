package com.exemple.quiz_app.AI.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public class GeminiRequestDto {
    @NotBlank(message="Le theme du quiz est obligatoire")
    @Size(min=3,max=200,message="Le theme doit contenir entre 3 et 200 caracteres")
    private String theme;
    @NotNull(message="Le nombre de questions est obligatoire")
    @Min(value=1,message="Le nombre de questions doit etre au minimum 1")
    @Max(value=20,message="Le nombre de  questions ne doit pas depasser 20")
    private Integer nbrQuestions;
    @Size(max=500,message="Les instructions supplementaires ne doivent pas deppasser 500 caracteres")
    private String instructionsSupp;
    @Min(value=1,message="La duree minimale est 1 min")
    @Max(value=180,message="La duree maximale est 180 min")
    private Integer durationMinutes;
    private String difficulty;
    public GeminiRequestDto(){}
    public GeminiRequestDto(String theme,Integer nbrQuestions){
        this.theme=theme;
        this.nbrQuestions=nbrQuestions;
    }
    public GeminiRequestDto(String theme,Integer nbrQuestions,String instrssupp,Integer durationMinutes,String difficulty ) {
        this.theme = theme;
        this.nbrQuestions=nbrQuestions;
        this.instructionsSupp=instrssupp;
        this.durationMinutes=durationMinutes;
        this.difficulty=difficulty;
    }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public Integer getNbrQuestions() { return nbrQuestions; }
    public void setNbrQuestions(Integer nbrQuestions) { this.nbrQuestions = nbrQuestions; }
    public String getInstructionsSupp() { return instructionsSupp; }
    public void setInstructionsSupp(String instructionsSupp) { this.instructionsSupp = instructionsSupp; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    @Override
    public String toString() {
        return "GeminiRequestDto{" +
                "theme='" + theme + '\'' +
                ", nbrQuestions=" + nbrQuestions +
                ", instructionsSupp='" + instructionsSupp + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}
