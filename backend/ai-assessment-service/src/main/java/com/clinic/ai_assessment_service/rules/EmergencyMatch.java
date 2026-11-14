package com.clinic.ai_assessment_service.rules;

import java.util.List;

public record EmergencyMatch(boolean triggered, List<String> ruleIds) {
}
