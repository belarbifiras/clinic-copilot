package com.clinic.ai_assessment_service.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TriageSessionRepository extends JpaRepository<TriageSession, UUID> {

    /** Accès limité au propriétaire : un patient ne lit que ses propres sessions. */
    Optional<TriageSession> findByIdAndPatientRef(UUID id, String patientRef);
}
