package com.exemple.quiz_app.AI.service;

import com.exemple.quiz_app.AI.client.GeminiApiClient;
import com.exemple.quiz_app.AI.dto.FeedbackRequestDto;
import com.exemple.quiz_app.AI.dto.FeedbackResponseDto;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.exemple.quiz_app.question.entity.Question;
import com.exemple.quiz_app.question.repository.QuestionRepository;
import com.exemple.quiz_app.quiz.entity.Quiz;
import com.exemple.quiz_app.quiz.repository.QuizRepository;
import com.exemple.quiz_app.reponse.entity.Reponse;
import com.exemple.quiz_app.reponse.repository.ReponseRepository;
import com.exemple.quiz_app.resultat.entity.Resultat;
import com.exemple.quiz_app.resultat.repository.ResultatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiFeedbackService {

    @Autowired
    private GeminiApiClient geminiApiClient;

    @Autowired
    private ResultatRepository resultatRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ReponseRepository reponseRepository;

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

        // 🔥 Récupérer les questions et réponses pour un feedback plus précis
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        List<Reponse> reponses = reponseRepository.findByStudentAndQuestionQuizId(resultat.getStudent(), quiz.getId());

        String prompt = buildFeedbackPrompt(resultat, quiz, questions, reponses, request.getLanguage());
        String response = geminiApiClient.callGemini(prompt);
        return parseFeedbackResponse(response, resultat.getScorePercentage());
    }

    private String buildFeedbackPrompt(Resultat resultat, Quiz quiz, List<Question> questions, List<Reponse> reponses, String language) {
        String lang = "fr".equals(language) ? "francais" : "english";

        // 🔥 Construire le détail des questions/réponses
        StringBuilder questionsDetails = new StringBuilder();
        for (Question q : questions) {
            Reponse r = reponses.stream()
                    .filter(rep -> rep.getQuestion().getId().equals(q.getId()))
                    .findFirst()
                    .orElse(null);

            String status = (r != null && r.getIsCorrect()) ? "✅ CORRECT" : "❌ INCORRECT";
            String studentAnswer = (r != null && r.getStudentAnswer() != null) ? r.getStudentAnswer() : "Non repondue";

            questionsDetails.append(String.format("""
                
                Question: %s
                Reponse etudiant: %s
                Reponse correcte: %s
                Statut: %s
                Points: %d
                ---
                """,
                    q.getEnonce(),
                    studentAnswer,
                    q.getCorrectAnswerText(),
                    status,
                    q.getPoints()
            ));
        }

        return String.format("""
            Tu es un tuteur pedagogique. Analyse ces resultats d'etudiant et donne un feedback en %s.

            === INFORMATIONS GENERALES ===
            Etudiant: %s
            Quiz: %s
            Theme: %s
            Score: %.1f%%
            Points: %d/%d

            === DETAIL DES QUESTIONS ===
            %s

            === INSTRUCTIONS ===
            Reponds UNIQUEMENT au format JSON suivant:
            {
              "feedback": "message d'encouragement personnalise (2-3 phrases)",
              "strengths": "points forts identifies (1-2 phrases)",
              "weaknesses": "points a ameliorer (1-2 phrases)",
              "recommendations": "conseils d'etude specifiques (2-3 recommandations)",
              "suggestedQuiz": "suggestion du prochain quiz"
            }

            Sois encourageant, constructif et specifique.
            """,
                lang,
                resultat.getStudent().getNom(),
                quiz.getTitre(),
                quiz.getTheme() != null ? quiz.getTheme() : "General",
                resultat.getScorePercentage(),
                resultat.getEarnedPoints(),
                resultat.getTotalPoints(),
                questionsDetails.toString()
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
            fallback.setStrengths("Bonnes reponses sur certaines questions");
            fallback.setWeaknesses("Quelques erreurs a corriger");
            fallback.setScore(score);
            fallback.setGrade(score >= 70 ? "Bien" : "A ameliorer");
            return fallback;
        }
    }
}