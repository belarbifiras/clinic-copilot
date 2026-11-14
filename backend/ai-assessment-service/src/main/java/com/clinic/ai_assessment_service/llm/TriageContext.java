package com.clinic.ai_assessment_service.llm;

import java.util.List;

/** Contexte MINIMAL et pseudonymisé envoyé au modèle (jamais de nom, téléphone ou identifiant). */
public record TriageContext(
        String language,
        Integer age,
        String sex,
        List<String> allergies,
        List<String> history,
        String symptoms,
        Integer durationDays,
        Integer intensity,
        List<String> questions,
        List<String> answers) {
}
