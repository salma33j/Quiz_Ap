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

    private static final int MAX_GENERATION_ATTEMPTS = 2;

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
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String response = geminiApiClient.callGemini(prompt);
            try {
                return parseQuizResponse(response, getMatiere(request));
            } catch (RuntimeException e) {
                lastError = e;
                prompt = buildGenerationPrompt(request) + """

                    IMPORTANT:
                    La reponse precedente etait incomplete ou invalide.
                    Renvoie un JSON COMPLET et VALIDE, sans markdown, sans commentaire.
                    Utilise des questions et options courtes pour eviter une reponse coupee.
                    """;
            }
        }

        throw lastError != null
                ? new RuntimeException(lastError.getMessage())
                : new RuntimeException("Gemini a renvoye une reponse invalide. Veuillez reessayer.");
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return "";
    }

    private String getMatiere(QuizGenerationRequestDto request) {
        return firstText(request.getMatiere(), request.getTheme(), "Matiere non definie");
    }

    private String buildGenerationPrompt(QuizGenerationRequestDto request) {
        String matiere = getMatiere(request);
        String titre = firstText(request.getTitre(), "Quiz sans titre");
        String description = firstText(request.getDescription(), "Objectif pedagogique non precise");
        String classe = firstText(request.getClasse(), "classe non precisee");
        int numberOfQuestions = request.getNumberOfQuestions() != null ? request.getNumberOfQuestions() : 15;

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
            Tu es un assistant pedagogique pour enseignants.
            Cree exactement %d questions de %s.

            Contraintes du professeur:
            - Matiere: "%s"
            - Titre du quiz: "%s"
            - Description / besoins pedagogiques: "%s"
            - Classe et niveau des etudiants: "%s"
            - Difficulte demandee: %s
            Type demande: %s.

            Regles importantes:
            - Toutes les questions doivent rester strictement dans la matiere indiquee.
            - Les questions doivent servir le titre et la description fournis par le professeur.
            - Adapte le vocabulaire, les exemples et la profondeur au niveau de la classe.
            - Ne cree pas de questions hors sujet, trop generales ou d'un niveau different.
            - Le resultat doit contenir exactement %d questions, pas plus, pas moins.
            - Utilise des textes courts: questionText max 160 caracteres, chaque option max 90 caracteres.
            - Ne donne aucune explication hors JSON.
            - Le JSON doit etre complet et valide.

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
                numberOfQuestions,
                difficultyText,
                matiere,
                titre,
                description,
                classe,
                request.getDifficulty(),
                typeText,
                numberOfQuestions
        );
    }

    private QuizGenerationResponseDto parseQuizResponse(String response, String theme) {
        try {
            String cleanedResponse = extractJsonObject(response);
            QuizGenerationResponseDto dto = objectMapper.readValue(cleanedResponse, QuizGenerationResponseDto.class);
            if (dto.getQuestions() == null || dto.getQuestions().isEmpty()) {
                throw new RuntimeException("aucune question generee");
            }
            dto.setTheme(theme);
            dto.setNumberOfQuestions(dto.getQuestions().size());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Gemini a renvoye une reponse incomplete. Relancez la generation ou diminuez le nombre de questions.");
        }
    }

    private String extractJsonObject(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("reponse vide");
        }

        String cleaned = response.replace("```json", "").replace("```", "").trim();
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw new RuntimeException("json absent ou incomplet");
        }

        return cleaned.substring(firstBrace, lastBrace + 1);
    }
}
