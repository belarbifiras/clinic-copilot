package com.clinic.ai_assessment_service.llm;

public class OllamaUnavailableException extends Exception {
    public OllamaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
