package com.exemple.quiz_app.AI.client;

import com.exemple.quiz_app.AI.config.GeminiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiApiClient {

    @Autowired
    private GeminiConfig geminiConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 3;

    public String callGemini(String prompt) {
        if (!geminiConfig.isApiConfigured()) {
            throw new RuntimeException("API Gemini non configuree. Veuillez ajouter gemini.api.key dans application.properties");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = buildRequestBody(prompt);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        geminiConfig.getApiUrl(),
                        HttpMethod.POST,
                        entity,
                        String.class
                );
                return extractTextFromResponse(response.getBody());
            } catch (RestClientResponseException e) {
                if (!isTemporaryGeminiError(e) || attempt == MAX_ATTEMPTS) {
                    throw new RuntimeException(cleanGeminiHttpError(e));
                }
                lastError = new RuntimeException(cleanGeminiHttpError(e));
                sleepBeforeRetry(attempt);
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'appel a l'API Gemini. Veuillez reessayer.");
            }
        }

        throw lastError != null ? lastError : new RuntimeException("Gemini indisponible. Veuillez reessayer.");
    }

    private boolean isTemporaryGeminiError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        return status == 429 || status == 503;
    }

    private String cleanGeminiHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 429 || status == 503) {
            return "Gemini est temporairement surcharge. Veuillez reessayer dans quelques secondes.";
        }
        if (status == 400) {
            return "Requete Gemini invalide. Verifiez le modele et la cle API.";
        }
        if (status == 403) {
            return "Cle Gemini refusee. Creez une nouvelle cle API Gemini dans Google AI Studio.";
        }
        return "Erreur Gemini (" + status + "). Veuillez reessayer.";
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(700L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildRequestBody(String prompt) {
        try {
            String promptJson = objectMapper.writeValueAsString(prompt);
            return String.format("""
            {
              "contents": [{
                "parts": [{"text": %s}]
              }],
              "generationConfig": {
                "temperature": 0.2,
                "maxOutputTokens": 8192,
                "responseMimeType": "application/json"
              }
            }
            """, promptJson);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de preparer la requete Gemini.");
        }
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de la reponse Gemini: " + e.getMessage());
        }
    }
}
