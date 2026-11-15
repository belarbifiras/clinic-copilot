package com.clinic.ai_assessment_service.llm;

/** Sortie du modèle invalide. Le message contient seulement un code de raison (jamais le contenu). */
public class InvalidModelOutputException extends Exception {
    public InvalidModelOutputException(String reason) {
        super(reason);
    }
}
