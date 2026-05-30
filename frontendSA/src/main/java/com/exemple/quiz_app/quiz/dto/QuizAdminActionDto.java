package com.exemple.quiz_app.quiz.dto;

public class QuizAdminActionDto {
    private Long quizId;
    private String action;
    private String newExpirationDate;
    private String reason;

    public QuizAdminActionDto() {}

    public QuizAdminActionDto(Long quizId, String action, String newExpirationDate, String reason) {
        this.quizId = quizId;
        this.action = action;
        this.newExpirationDate = newExpirationDate;
        this.reason = reason;
    }

    public Long getQuizId() { return quizId; }
    public void setQuizId(Long quizId) { this.quizId = quizId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getNewExpirationDate() { return newExpirationDate; }
    public void setNewExpirationDate(String newExpirationDate) { this.newExpirationDate = newExpirationDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}