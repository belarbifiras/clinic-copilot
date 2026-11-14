package com.clinic.ai_assessment_service.domain;

/** Cause possible (orientation uniquement, jamais un diagnostic). */
public record PossibleCondition(String label, double likelihood) {
}
