package com.exemple.quiz_app.AI.service;

import com.exemple.quiz_app.AI.client.GeminiApiClient;
import com.exemple.quiz_app.AI.dto.FeedbackRequestDto;
import com.exemple.quiz_app.AI.dto.FeedbackResponseDto;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.resultat.entity.Resultat;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiFeedbackService {

    @Autowired
    private GeminiApiClient geminiApiClient;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FeedbackResponseDto generateFeedback(FeedbackRequestDto request) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() != Role.ETUDIANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Le feedback est disponible pour les etudiants uniquement");
        }

        Resultat resultat = resultatRepository.findById(request.getResultatId())
                .orElseThrow(() -> new RuntimeException("Resultat non trouve"));

        if (currentUser.getRole() == Role.ETUDIANT && !resultat.getStudent().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Vous ne pouvez pas voir les resultats d'un autre etudiant");
        }

        Quiz quiz = quizRepository.findById(resultat.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz non trouve"));

        String prompt = buildFeedbackPrompt(resultat, quiz, request.getLanguage());
        String response = geminiApiClient.callGemini(prompt);
        return parseFeedbackResponse(response, resultat.getScorePercentage());
    }

    private String buildFeedbackPrompt(Resultat resultat, Quiz quiz, String language) {
        String lang = "fr".equals(language) ? "francais" : "english";

        return String.format("""
            Tu es un tuteur pedagogique. Analyse ces resultats d'etudiant et donne un feedback en %s.

            Quiz: %s
            Theme: %s
            Score: %.1f%%
            Points: %d/%d

            Reponds UNIQUEMENT au format JSON suivant:
            {
              "feedback": "message d'encouragement personnalise",
              "strengths": "points forts de l'etudiant",
              "weaknesses": "points a ameliorer",
              "recommendations": "conseils d'etude specifiques",
              "suggestedQuiz": "suggestion du prochain quiz"
            }

            Sois encourageant et constructif.
            """,
                lang,
                quiz.getTitre(),
                quiz.getTheme() != null ? quiz.getTheme() : "General",
                resultat.getScorePercentage(),
                resultat.getEarnedPoints(),
                resultat.getTotalPoints()
        );
    }

    private FeedbackResponseDto parseFeedbackResponse(String response, Double score) {
        try {
            String cleanedResponse = response.replace("```json", "").replace("```", "").trim();
            FeedbackResponseDto dto = objectMapper.readValue(cleanedResponse, FeedbackResponseDto.class);
            dto.setScore(score);

            if (score >= 90) dto.setGrade("A+");
            else if (score >= 80) dto.setGrade("A");
            else if (score >= 70) dto.setGrade("B");
            else if (score >= 60) dto.setGrade("C");
            else if (score >= 50) dto.setGrade("D");
            else dto.setGrade("F");

            return dto;
        } catch (Exception e) {
            FeedbackResponseDto fallback = new FeedbackResponseDto();
            fallback.setFeedback(score >= 70 ? "Excellent travail !" : "Continuez vos efforts !");
            fallback.setRecommendations("Revoyez les questions ou vous avez eu faux.");
            fallback.setScore(score);
            fallback.setGrade(score >= 70 ? "Bien" : "A ameliorer");
            return fallback;
        }
    }
}