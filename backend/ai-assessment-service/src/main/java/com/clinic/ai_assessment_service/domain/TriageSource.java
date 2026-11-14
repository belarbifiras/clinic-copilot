package com.clinic.ai_assessment_service.domain;

/** D'où vient le résultat : règles déterministes, LLM, ou repli de sécurité. */
public enum TriageSource {
    RULES,
    LLM,
    FALLBACK
}
