package com.clinic.ai_assessment_service.events;

/** Point d'extension : une implémentation RabbitMQ pourra remplacer l'implémentation de log. */
public interface TriageEventPublisher {
    void publish(TriageEvent event);
}
