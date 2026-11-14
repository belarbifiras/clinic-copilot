package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.domain.TriageAssessment;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecautionPolicyTest {

    private final PrecautionPolicy policy = new PrecautionPolicy(0.5);

    private static TriageAssessment assessment(UrgencyLevel level, double confidence, List<String> redFlags) {
        return new TriageAssessment(level, confidence, List.of(), "general_medicine",
                redFlags, "explication", "résumé", List.of());
    }

    @Test
    void lowConfidenceRaisesOneStep() {
        var r = policy.apply(assessment(UrgencyLevel.N5, 0.3, List.of()));
        assertEquals(UrgencyLevel.N4, r.assessment().urgencyLevel());
        assertTrue(r.adjustments().contains("LOW_CONFIDENCE_RAISED_ONE_STEP"));
    }

    @Test
    void lowConfidenceNeverAutoEscalatesToN1() {
        var r = policy.apply(assessment(UrgencyLevel.N2, 0.1, List.of()));
        assertEquals(UrgencyLevel.N2, r.assessment().urgencyLevel());
    }

    @Test
    void redFlagsImposeAtLeastN3() {
        var r = policy.apply(assessment(UrgencyLevel.N5, 0.9, List.of("fever for 5 days")));
        assertEquals(UrgencyLevel.N3, r.assessment().urgencyLevel());
    }

    @Test
    void confidentBenignResultIsUnchanged() {
        var r = policy.apply(assessment(UrgencyLevel.N4, 0.9, List.of()));
        assertEquals(UrgencyLevel.N4, r.assessment().urgencyLevel());
        assertTrue(r.adjustments().isEmpty());
    }
}
