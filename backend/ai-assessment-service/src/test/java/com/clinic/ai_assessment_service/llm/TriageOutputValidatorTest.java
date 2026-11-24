package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.domain.TriageAssessment;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TriageOutputValidatorTest {

    private final TriageOutputValidator validator =
            new TriageOutputValidator(List.of("general_medicine", "cardiology", "ent"));

    private static String json(String urgency, String confidence, String specialty, String explanation, String extra) {
        return json(urgency, confidence, specialty, explanation, true, extra);
    }

    private static String json(String urgency, String confidence, String specialty, String explanation,
                               boolean consultationRecommended, String extra) {
        return """
                {
                  "urgency_level": "%s",
                  "confidence": %s,
                  "possible_conditions": [{"label": "Viral throat infection", "likelihood": 0.6}],
                  "recommended_specialty": "%s",
                  "red_flags_detected": [],
                  "patient_explanation": "%s",
                  "doctor_summary": "Sore throat for 3 days, mild fever.",
                  "followup_questions": [],
                  "consultation_recommended": %s%s
                }
                """.formatted(urgency, confidence, specialty, explanation, consultationRecommended, extra);
    }

    @Test
    void acceptsValidOutput() throws Exception {
        TriageAssessment a = validator.parseAndValidate(
                json("N4", "0.8", "ent", "Ceci est une orientation, pas un diagnostic.", ""));
        assertEquals(UrgencyLevel.N4, a.urgencyLevel());
        assertEquals("ent", a.recommendedSpecialty());
        assertEquals(true, a.requiresConsultation());
    }

    @Test
    void rejectsNotJson() {
        assertThrows(InvalidModelOutputException.class, () -> validator.parseAndValidate("hello"));
    }

    @Test
    void rejectsUnknownUrgencyLevel() {
        assertThrows(InvalidModelOutputException.class,
                () -> validator.parseAndValidate(json("N9", "0.8", "ent", "ok", "")));
    }

    @Test
    void rejectsUnknownSpecialty() {
        assertThrows(InvalidModelOutputException.class,
                () -> validator.parseAndValidate(json("N4", "0.8", "astrology", "ok", "")));
    }

    @Test
    void rejectsConfidenceOutOfRange() {
        assertThrows(InvalidModelOutputException.class,
                () -> validator.parseAndValidate(json("N4", "1.7", "ent", "ok", "")));
    }

    @Test
    void rejectsExtraField() {
        assertThrows(InvalidModelOutputException.class,
                () -> validator.parseAndValidate(json("N4", "0.8", "ent", "ok", ", \"diagnosis\": \"flu\"")));
    }

    @Test
    void rejectsDosageMention() {
        assertThrows(InvalidModelOutputException.class,
                () -> validator.parseAndValidate(json("N4", "0.8", "ent", "Prenez 500 mg toutes les 6 heures", "")));
    }

    @Test
    void rejectsDrugName() {
        assertThrows(InvalidModelOutputException.class,
                () -> validator.parseAndValidate(json("N4", "0.8", "ent", "Vous pouvez prendre du paracétamol", "")));
    }

    @Test
    void acceptsGeneralMedicationInformationWhenPatientAskedAboutIt() throws Exception {
        TriageAssessment a = validator.parseAndValidate(
                json("N5", "0.9", "general_medicine", "Le Doliprane peut soulager la fièvre.", false, ""), true);

        assertEquals(false, a.requiresConsultation());
    }

    @Test
    void stillRejectsMedicationDosagesForGeneralInformation() {
        assertThrows(InvalidModelOutputException.class, () -> validator.parseAndValidate(
                json("N5", "0.9", "general_medicine", "Le Doliprane peut soulager la fièvre à 500 mg.", false, ""),
                true));
    }
}
