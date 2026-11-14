package com.clinic.ai_assessment_service.events;

import com.clinic.ai_assessment_service.domain.TriageSource;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;

import java.util.UUID;

/** type : COMPLETED, URGENT (N1/N2) ou ADMIN_ALERT (repli utilisé). Aucune donnée de santé dans l'événement. */
public record TriageEvent(String type, UUID sessionId, UrgencyLevel urgencyLevel, TriageSource source) {
}
