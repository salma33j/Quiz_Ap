package com.exemple.quiz_app.AI.client;

import com.exemple.quiz_app.AI.config.GeminiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiApiClient {

    @Autowired
    private GeminiConfig geminiConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callGemini(String prompt) {
        if (!geminiConfig.isApiConfigured()) {
            throw new RuntimeException("API Gemini non configuree. Veuillez ajouter gemini.api.key dans application.properties");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = buildRequestBody(prompt);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    geminiConfig.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            return extractTextFromResponse(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'appel a l'API Gemini: " + e.getMessage());
        }
    }

    private String buildRequestBody(String prompt) {
        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");
        return String.format("""
            {
              "contents": [{
                "parts": [{"text": "%s"}]
              }],
              "generationConfig": {
                "temperature": 0.7,
                "maxOutputTokens": 2048
              }
            }
            """, escapedPrompt);
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