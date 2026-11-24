package com.clinic.ai_assessment_service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Résultat de triage validé. Les noms JSON suivent le cahier des charges (snake_case). */
public record TriageAssessment(
        @JsonProperty("urgency_level") UrgencyLevel urgencyLevel,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("possible_conditions") List<PossibleCondition> possibleConditions,
        @JsonProperty("recommended_specialty") String recommendedSpecialty,
        @JsonProperty("red_flags_detected") List<String> redFlagsDetected,
        @JsonProperty("patient_explanation") String patientExplanation,
        @JsonProperty("doctor_summary") String doctorSummary,
        @JsonProperty("followup_questions") List<String> followupQuestions,
        @JsonProperty("consultation_recommended") Boolean consultationRecommended) {

    /** Older persisted results predate the flag and keep the previous triage behavior. */
    public boolean requiresConsultation() {
        return consultationRecommended == null || consultationRecommended;
    }

    public TriageAssessment withUrgency(UrgencyLevel level) {
        return new TriageAssessment(level, confidence, possibleConditions, recommendedSpecialty,
                redFlagsDetected, patientExplanation, doctorSummary, followupQuestions, consultationRecommended);
    }

    public TriageAssessment withoutFollowups() {
        return new TriageAssessment(urgencyLevel, confidence, possibleConditions, recommendedSpecialty,
                redFlagsDetected, patientExplanation, doctorSummary, List.of(), consultationRecommended);
    }
}
