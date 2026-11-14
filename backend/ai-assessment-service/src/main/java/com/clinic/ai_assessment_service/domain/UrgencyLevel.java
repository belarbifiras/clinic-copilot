package com.clinic.ai_assessment_service.domain;

/** N1 = vital (le plus urgent) ... N5 = conseil / suivi (le moins urgent). */
public enum UrgencyLevel {
    N1, N2, N3, N4, N5;

    /** Un cran plus urgent (N5 -> N4 ...). N1 reste N1. */
    public UrgencyLevel raiseOneStep() {
        return ordinal() == 0 ? this : values()[ordinal() - 1];
    }
}
