package com.clinic.ai_assessment_service.triage;

import com.clinic.ai_assessment_service.directory.DoctorSuggestion;
import com.clinic.ai_assessment_service.domain.TriageAssessment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Réponse API.
 * status=NEEDS_CLARIFICATION -> clarificationQuestions + preliminaryExplanation (explication provisoire, sans niveau).
 * emergency=true   -> le frontend affiche l'écran d'urgence et l'action d'appel (numéro local issu d'une configuration validée).
 * interimAdvice    -> consignes d'attente (textes FIXES, brouillon à valider par un médecin), seulement en N1.
 * bookingAllowed   -> false pour N1 (pas de réservation en ligne).
 * suggestedDoctors -> médecins les plus proches (annuaire), seulement si la position a été fournie et hors N1.
 * disclaimer       -> à afficher AVANT et APRÈS le résultat.
 */
public record TriageSessionResponse(
        UUID sessionId,
        String status,
        String language,
        List<String> clarificationQuestions,
        String preliminaryExplanation,
        TriageAssessment assessment,
        String source,
        boolean emergency,
        boolean bookingAllowed,
        List<String> interimAdvice,
        List<DoctorSuggestion> suggestedDoctors,
        String modelName,
        String promptVersion,
        String disclaimer,
        Instant createdAt) {
}