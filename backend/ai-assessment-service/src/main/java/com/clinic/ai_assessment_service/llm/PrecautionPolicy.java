package com.clinic.ai_assessment_service.llm;

import com.clinic.ai_assessment_service.config.AiProperties;
import com.clinic.ai_assessment_service.domain.TriageAssessment;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Principe de précaution appliqué APRÈS validation du JSON :
 * - confiance faible -> un cran plus urgent (sans jamais passer à N1 automatiquement : N1 vient des règles ou du modèle) ;
 * - signes d'alarme détectés -> au minimum N3.
 * Ces seuils sont des choix d'ingénierie à FAIRE VALIDER cliniquement.
 */
@Component
public class PrecautionPolicy {

    public record PolicyResult(TriageAssessment assessment, List<String> adjustments) {
    }

    private final double lowConfidenceThreshold;

    @Autowired
    public PrecautionPolicy(AiProperties props) {
        this(props.lowConfidenceThreshold());
    }

    public PrecautionPolicy(double lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    public PolicyResult apply(TriageAssessment a) {
        UrgencyLevel level = a.urgencyLevel();
        List<String> adjustments = new ArrayList<>();

        if (a.confidence() < lowConfidenceThreshold && level.ordinal() > UrgencyLevel.N2.ordinal()) {
            level = level.raiseOneStep();
            adjustments.add("LOW_CONFIDENCE_RAISED_ONE_STEP");
        }
        if (!a.redFlagsDetected().isEmpty() && level.ordinal() > UrgencyLevel.N3.ordinal()) {
            level = UrgencyLevel.N3;
            adjustments.add("RED_FLAGS_MIN_N3");
        }
        return new PolicyResult(a.withUrgency(level), List.copyOf(adjustments));
    }
}
