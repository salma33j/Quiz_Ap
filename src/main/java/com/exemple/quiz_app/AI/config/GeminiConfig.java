package com.exemple.quiz_app.AI.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfig.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    // ✅ Valeur par défaut corrigée
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent}")
    private String apiUrl;

    @PostConstruct
    void logResolvedGeminiEndpoint() {
        log.info("✅ Gemini URL utilisée : {}", apiUrl);
        log.info("✅ Clé configurée : {}", apiKey != null && !apiKey.isEmpty() ? "OUI" : "NON");
    }

    public String getApiKey() { return apiKey; }

    public String getApiUrl() { return apiUrl + "?key=" + apiKey; }

    public boolean isApiConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }
}