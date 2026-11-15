package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.domain.FallbackReason;
import com.clinic.ai_assessment_service.domain.TriageAssessment;

import java.util.List;

/** Résultat d'un passage par le modèle : soit une évaluation validée, soit une raison de repli. */
public record ModelOutcome(
        TriageAssessment assessment,
        FallbackReason fallbackReason,
        String modelName,
        String modelDigest,
        List<String> adjustments) {

    public static ModelOutcome ok(TriageAssessment a, String model, String digest, List<String> adjustments) {
        return new ModelOutcome(a, null, model, digest, adjustments);
    }

    public static ModelOutcome fallback(FallbackReason reason, String model) {
        return new ModelOutcome(null, reason, model, "unknown", List.of());
    }
}
