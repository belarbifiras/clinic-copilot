package com.clinic.ai_assessment_service.persistence;

import com.clinic.ai_assessment_service.domain.FallbackReason;
import com.clinic.ai_assessment_service.domain.SessionStatus;
import com.clinic.ai_assessment_service.domain.TriageSource;
import com.clinic.ai_assessment_service.domain.UrgencyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Session de triage. Contient des données de santé : ne jamais les écrire dans les logs.
 * Champs publics volontairement (entité interne simple) ; les colonnes suivent la migration V1.
 */
@Entity
@Table(name = "triage_session")
public class TriageSession {

    @Id
    public UUID id;

    @Column(nullable = false, length = 64)
    public String patientRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public SessionStatus status;

    @Column(nullable = false, length = 5)
    public String language;

    @Column(nullable = false, columnDefinition = "text")
    public String symptoms;

    public Integer age;

    @Column(length = 16)
    public String sex;

    @Column(columnDefinition = "text")
    public String allergiesJson;

    @Column(columnDefinition = "text")
    public String historyJson;

    public Integer durationDays;

    public Integer intensity;

    @Column(columnDefinition = "text")
    public String clarificationQuestionsJson;

    @Column(columnDefinition = "text")
    public String clarificationAnswersJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    public UrgencyLevel urgencyLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    public TriageSource source;

    public Double confidence;

    @Column(length = 80)
    public String recommendedSpecialty;

    @Column(columnDefinition = "text")
    public String resultJson;

    @Column(length = 100)
    public String modelName;

    @Column(length = 100)
    public String modelDigest;

    @Column(length = 32)
    public String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    public FallbackReason fallbackReason;

    @Column(nullable = false)
    public boolean adminAlerted;

    @Column(columnDefinition = "text")
    public String precautionAdjustments;

    @Column(nullable = false)
    public Instant createdAt;

    @Column(nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
