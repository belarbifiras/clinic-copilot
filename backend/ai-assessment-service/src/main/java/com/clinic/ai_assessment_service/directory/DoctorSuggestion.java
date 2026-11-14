package com.clinic.ai_assessment_service.directory;

/** Médecin proposé (données publiques de l'annuaire). distanceKm = distance à vol d'oiseau, peut être null. */
public record DoctorSuggestion(String id, String displayName, String city, String governorate, Double distanceKm) {
}
