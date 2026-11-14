package com.clinic.ai_assessment_service.triage;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnswerRequest(
        @NotNull @Size(min = 1, max = 5) List<@NotBlank @Size(max = 500) String> answers,
        // Voir StartTriageRequest : position facultative, non conservée.
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}