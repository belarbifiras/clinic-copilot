package com.clinic.ai_assessment_service.domain;

public enum FallbackReason {
    OLLAMA_UNAVAILABLE,
    INVALID_OUTPUT,
    BUSY
}
