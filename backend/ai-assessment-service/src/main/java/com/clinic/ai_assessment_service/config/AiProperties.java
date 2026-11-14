package com.clinic.ai_assessment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Paramètres du module IA (voir application.yml, préfixe "ai"). */
@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String promptVersion,
        String doctorSummaryLanguage,
        double lowConfidenceThreshold,
        boolean clarificationEnabled,
        int maxConcurrentRequests,
        int queueWaitSeconds,
        Llm llm,
        List<String> allowedSpecialties) {

    public record Llm(
            String baseUrl,
            String apiKey,
            String model,
            int connectTimeoutSeconds,
            int readTimeoutSeconds) {
    }
}