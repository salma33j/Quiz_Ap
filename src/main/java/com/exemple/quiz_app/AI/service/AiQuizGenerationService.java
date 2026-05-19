package com.exemple.quiz_app.AI.service;

import com.exemple.quiz_app.AI.client.GeminiApiClient;
import com.exemple.quiz_app.AI.dto.QuizGenerationRequestDto;
import com.exemple.quiz_app.AI.dto.QuizGenerationResponseDto;
import com.exemple.quiz_app.auth.model.Role;
import com.exemple.quiz_app.auth.model.User;
import com.exemple.quiz_app.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiQuizGenerationService {

    @Autowired
    private GeminiApiClient geminiApiClient;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizGenerationResponseDto generateQuizContent(QuizGenerationRequestDto request) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() != Role.ENSEIGNANT && currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Seul un enseignant peut generer un quiz via IA");
        }

        String prompt = buildGenerationPrompt(request);
        String response = geminiApiClient.callGemini(prompt);
        return parseQuizResponse(response, request.getTheme());
    }

    private String buildGenerationPrompt(QuizGenerationRequestDto request) {
        String difficultyText = "";
        if ("FACILE".equals(request.getDifficulty())) {
            difficultyText = "niveau debutant, questions simples";
        } else if ("DIFFICILE".equals(request.getDifficulty())) {
            difficultyText = "niveau avance, questions complexes";
        } else {
            difficultyText = "niveau intermediaire";
        }

        String typeText = switch (request.getType() != null ? request.getType() : "ALL") {
            case "QCM", "MCQ" -> "OBLIGATOIREMENT toutes les questions doivent etre de type MCQ, aucune TRUE_FALSE, aucune TEXT";
            case "TRUE_FALSE" -> "OBLIGATOIREMENT toutes les questions doivent etre de type TRUE_FALSE, aucune MCQ, aucune TEXT";
            case "TEXT" -> "OBLIGATOIREMENT toutes les questions doivent etre de type TEXT, aucune MCQ, aucune TRUE_FALSE";
            default -> "un melange equilibre de QCM, Vrai/Faux et questions ouvertes";
        };

        return String.format("""
            Tu es un generateur de quiz. Cree %d questions de %s sur le theme: "%s".
            Type demande: %s.

            Pour chaque question, fournis:
            1. Le texte de la question
            2. Le type (MCQ pour QCM, TRUE_FALSE pour Vrai/Faux, TEXT pour question ouverte)
            3. Pour les QCM, fournis 4 options
            4. La reponse correcte. Pour MCQ et TRUE_FALSE, utilise uniquement la lettre A, B, C ou D.
            5. Le nombre de points (1 point par question)

            Reponds UNIQUEMENT au format JSON suivant, sans aucun autre texte:
            {
              "questions": [
                {
                  "questionText": "...",
                  "type": "MCQ | TRUE_FALSE | TEXT",
                  "options": ["option1", "option2", "option3", "option4"],
                  "correctAnswer": "A",
                  "points": 1
                }
              ]
            }

            Si Type demande impose TRUE_FALSE, chaque question doit avoir:
            "type": "TRUE_FALSE", "options": ["Vrai", "Faux"], "correctAnswer": "A" ou "B".

            Si Type demande impose TEXT, chaque question doit avoir:
            "type": "TEXT", "options": [], "correctAnswer": une reponse texte.
            """,
                request.getNumberOfQuestions(),
                difficultyText,
                request.getTheme(),
                typeText
        );
    }

    private QuizGenerationResponseDto parseQuizResponse(String response, String theme) {
        try {
            String cleanedResponse = response.replace("```json", "").replace("```", "").trim();
            QuizGenerationResponseDto dto = objectMapper.readValue(cleanedResponse, QuizGenerationResponseDto.class);
            dto.setTheme(theme);
            dto.setNumberOfQuestions(dto.getQuestions().size());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du parsing de la reponse Gemini: " + e.getMessage());
        }
    }
}
