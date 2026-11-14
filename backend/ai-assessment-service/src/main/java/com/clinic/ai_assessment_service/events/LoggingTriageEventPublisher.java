package com.clinic.ai_assessment_service.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Implémentation provisoire : écrit seulement dans les logs (RabbitMQ pas encore branché). */
@Component
public class LoggingTriageEventPublisher implements TriageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingTriageEventPublisher.class);

    @Override
    public void publish(TriageEvent e) {
        log.info("triage_event type={} session={} level={} source={}",
                e.type(), e.sessionId(), e.urgencyLevel(), e.source());
    }
}
