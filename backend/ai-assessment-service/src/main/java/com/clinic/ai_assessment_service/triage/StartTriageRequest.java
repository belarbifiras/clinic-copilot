package com.clinic.ai_assessment_service.triage;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pas de nom ni de téléphone : le service n'en a pas besoin.
 * (Plus tard, age/sex/allergies/antécédents viendront du profil via identity-profile-service.)
 */
public record StartTriageRequest(
        @NotBlank @Size(max = 2000) String symptoms,
        @NotNull @Pattern(regexp = "ar|fr|en") String language,
        @Min(0) @Max(120) Integer age,
        @Pattern(regexp = "MALE|FEMALE|OTHER") String sex,
        @Size(max = 20) List<@Size(max = 100) String> allergies,
        @Size(max = 20) List<@Size(max = 150) String> medicalHistory,
        @Min(0) @Max(3650) Integer durationDays,
        @Min(1) @Max(10) Integer intensity,
        // Position du patient, FACULTATIVE et avec son accord : sert uniquement à chercher le médecin le plus proche.
        // Jamais enregistrée, jamais journalisée, jamais envoyée au modèle.
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}